package com.ros.ewallet.service;

import com.ros.ewallet.common.Constants;
import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.exception.ForbiddenException;
import com.ros.ewallet.exception.InsufficientFundsException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.rail.PaymentRail;
import com.ros.ewallet.rail.RailResult;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ros.ewallet.common.Constants.TYPE_REVERSE;
import static com.ros.ewallet.common.Constants.TYPE_TOP_UP;
import static com.ros.ewallet.common.Constants.TYPE_TRANSFER;
import static com.ros.ewallet.common.Constants.TYPE_WITHDRAW;
import static com.ros.ewallet.common.MessageKeys.*;
import static com.ros.ewallet.service.IdempotencyService.OP_REVERSE;

/**
 * Compensating reverse of SUCCESS Transfer / Top-up / Withdraw within a time window.
 */
@Slf4j
@Service
public class TransactionReverseService {

    private static final Set<Long> REVERSIBLE_TYPES = Set.of(TYPE_TRANSFER, TYPE_WITHDRAW, TYPE_TOP_UP);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final PaymentRail paymentRail;
    private final IdempotencyService idempotencyService;
    private final TransactionQuotaService transactionQuotaService;
    private final TransactionLimitService transactionLimitService;
    private final SpendRequestService spendRequestService;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final TransactionLimitProperties limitProperties;
    private final Clock clock;
    private final ActivityLogService activityLogService;

    public TransactionReverseService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            TransactionService transactionService,
            WalletService walletService,
            LedgerService ledgerService,
            PaymentRail paymentRail,
            IdempotencyService idempotencyService,
            TransactionQuotaService transactionQuotaService,
            TransactionLimitService transactionLimitService,
            @Lazy SpendRequestService spendRequestService,
            SecurityAccess securityAccess,
            MessageSourceConfig messageConfig,
            TransactionLimitProperties limitProperties,
            Clock clock,
            ActivityLogService activityLogService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.ledgerService = ledgerService;
        this.paymentRail = paymentRail;
        this.idempotencyService = idempotencyService;
        this.transactionQuotaService = transactionQuotaService;
        this.transactionLimitService = transactionLimitService;
        this.spendRequestService = spendRequestService;
        this.securityAccess = securityAccess;
        this.messageConfig = messageConfig;
        this.limitProperties = limitProperties;
        this.clock = clock;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public CommandResponse reverse(long transactionId, String idempotencyKey) {
        final Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(orgId, OrganizationRole.OWNER, OrganizationRole.ADMIN);

        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_REVERSE, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        Transaction original = loadEligibleOriginal(transactionId, orgId);
        transactionQuotaService.assertWithinQuota(orgId);

        if (transactionLimitService.requiresDualControl(orgId, original.getAmount())) {
            return spendRequestService.createPendingReverse(orgId, original);
        }

        return executeReverse(original.getId(), idempotencyKey, orgId);
    }

    /**
     * Executes reverse immediately (below-threshold path and dual-control approve).
     */
    @Transactional
    public CommandResponse executeReverse(long originalTransactionId, String idempotencyKey, Long orgId) {
        final Long userId = securityAccess.currentUser().getId();
        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_REVERSE, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        Transaction original = loadEligibleOriginal(originalTransactionId, orgId);
        transactionQuotaService.assertWithinQuota(orgId);

        long originalTypeId = original.getType().getId();
        Wallet fromWallet = original.getFromWallet();
        Wallet toWallet = original.getToWallet();
        BigDecimal amount = original.getAmount();

        if (originalTypeId == TYPE_TOP_UP) {
            requireRailSuccess(
                    paymentRail.refundTopUp(toWallet.getIban(), amount, reverseDescription(original)),
                    "top-up");
        } else if (originalTypeId == TYPE_WITHDRAW) {
            requireRailSuccess(
                    paymentRail.refundWithdraw(fromWallet.getIban(), amount, reverseDescription(original)),
                    "withdraw");
        }

        applyBalanceReverse(originalTypeId, fromWallet, toWallet, amount);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(amount);
        request.setDescription(reverseDescription(original));
        request.setFromWalletIban(fromWallet.getIban());
        request.setToWalletIban(toWallet.getIban());
        request.setTypeId(TYPE_REVERSE);

        Transaction reverseTx = transactionService.createEntity(request);
        reverseTx.setReversesTransaction(original);
        transactionRepository.save(reverseTx);

        ledgerService.postReverse(
                reverseTx, originalTypeId, fromWallet, toWallet, amount, currencyOf(fromWallet));

        idempotencyService.remember(orgId, userId, OP_REVERSE, idempotencyKey, reverseTx.getId());
        activityLogService.record(
                orgId,
                userId,
                ActivityEventType.TX_REVERSE,
                "Transaction reversed",
                Map.of(
                        "reverseTransactionId", reverseTx.getId(),
                        "sourceTransactionId", original.getId()));
        return CommandResponse.completed(reverseTx.getId());
    }

    boolean isWithinReverseWindow(Instant createdAt) {
        int hours = Math.max(0, limitProperties.getReverseWindowHours());
        Instant cutoff = clock.instant().minus(Duration.ofHours(hours));
        return !createdAt.isBefore(cutoff);
    }

    boolean isReversibleType(long typeId) {
        return REVERSIBLE_TYPES.contains(typeId);
    }

    private Transaction loadEligibleOriginal(long transactionId, Long orgId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementFoundException(
                        messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND)));

        securityAccess.requireWalletInActiveOrg(original.getFromWallet());
        if (original.getFromWallet().getOrganization() == null
                || !orgId.equals(original.getFromWallet().getOrganization().getId())) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_TRANSACTION_NOT_FOUND));
        }

        if (original.getStatus() != Status.SUCCESS) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_REVERSE_STATUS));
        }
        if (original.getType() == null || !isReversibleType(original.getType().getId())) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_REVERSE_TYPE));
        }
        if (transactionRepository.findByReversesTransaction_Id(original.getId()).isPresent()) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_REVERSE_ALREADY));
        }
        if (!isWithinReverseWindow(original.getCreatedAt())) {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_REVERSE_WINDOW));
        }
        return original;
    }

    private void applyBalanceReverse(long originalTypeId, Wallet fromWallet, Wallet toWallet,
                                     BigDecimal amount) {
        if (originalTypeId == TYPE_TRANSFER) {
            Wallet lockedTo = walletService.getByIbanForUpdate(toWallet.getIban());
            Wallet lockedFrom = walletService.getByIban(fromWallet.getIban());
            if (lockedTo.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
            }
            lockedTo.setBalance(lockedTo.getBalance().subtract(amount));
            lockedFrom.setBalance(lockedFrom.getBalance().add(amount));
            walletRepository.save(lockedTo);
            walletRepository.save(lockedFrom);
            log.info(messageConfig.getMessage(INFO_WALLET_BALANCES_UPDATED, lockedFrom.getId(), lockedTo.getId()));
        } else if (originalTypeId == TYPE_TOP_UP) {
            Wallet lockedTo = walletService.getByIbanForUpdate(toWallet.getIban());
            if (lockedTo.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
            }
            lockedTo.setBalance(lockedTo.getBalance().subtract(amount));
            walletRepository.save(lockedTo);
            log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, lockedTo.getId()));
        } else if (originalTypeId == TYPE_WITHDRAW) {
            Wallet lockedFrom = walletService.getByIbanForUpdate(fromWallet.getIban());
            lockedFrom.setBalance(lockedFrom.getBalance().add(amount));
            walletRepository.save(lockedFrom);
            log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, lockedFrom.getId()));
        } else {
            throw new ForbiddenException(messageConfig.getMessage(ERROR_REVERSE_TYPE));
        }
    }

    private static void requireRailSuccess(RailResult rail, String kind) {
        if (rail.status() != Status.SUCCESS) {
            throw new IllegalStateException("Payment rail did not complete " + kind + " refund: " + rail.status());
        }
    }

    private static String reverseDescription(Transaction original) {
        String base = "Reverse of #" + original.getId();
        return base.length() <= 50 ? base : base.substring(0, 50);
    }

    private static String currencyOf(Wallet wallet) {
        return wallet.getCurrency() != null ? wallet.getCurrency() : Constants.CURRENCY_VND;
    }
}

package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Customer;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.ActivityEventType;
import com.ros.ewallet.domain.enums.CustomerStatus;
import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.domain.enums.WalletOwnerType;
import com.ros.ewallet.dto.mapper.WalletRequestMapper;
import com.ros.ewallet.dto.mapper.WalletResponseMapper;
import com.ros.ewallet.dto.mapper.WalletTransactionRequestMapper;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.dto.request.WalletRequest;
import com.ros.ewallet.dto.response.CommandResponse;
import com.ros.ewallet.dto.response.WalletResponse;
import com.ros.ewallet.exception.ElementAlreadyExistsException;
import com.ros.ewallet.exception.InsufficientFundsException;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.rail.PaymentRail;
import com.ros.ewallet.rail.RailResult;
import com.ros.ewallet.repository.CustomerRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ros.ewallet.common.Constants.CURRENCY_VND;
import static com.ros.ewallet.common.Constants.TYPE_TOP_UP;
import static com.ros.ewallet.common.Constants.TYPE_TRANSFER;
import static com.ros.ewallet.common.Constants.TYPE_WITHDRAW;
import static com.ros.ewallet.common.MessageKeys.*;
import static com.ros.ewallet.service.IdempotencyService.*;

/**
 * Service used for Wallet related operations (org-scoped).
 */
@Slf4j
@Service
public class WalletService {

    private final MessageSourceConfig messageConfig;
    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;
    private final WalletRequestMapper walletRequestMapper;
    private final WalletResponseMapper walletResponseMapper;
    private final WalletTransactionRequestMapper walletTransactionRequestMapper;
    private final IbanGenerator ibanGenerator;
    private final SecurityAccess securityAccess;
    private final PaymentRail paymentRail;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final TransactionLimitService transactionLimitService;
    private final TransactionQuotaService transactionQuotaService;
    private final OrganizationService organizationService;
    private final SpendRequestService spendRequestService;
    private final ActivityLogService activityLogService;

    private static final int IBAN_GENERATE_MAX_ATTEMPTS = 10;

    public WalletService(
            MessageSourceConfig messageConfig,
            WalletRepository walletRepository,
            CustomerRepository customerRepository,
            TransactionService transactionService,
            WalletRequestMapper walletRequestMapper,
            WalletResponseMapper walletResponseMapper,
            WalletTransactionRequestMapper walletTransactionRequestMapper,
            IbanGenerator ibanGenerator,
            SecurityAccess securityAccess,
            PaymentRail paymentRail,
            LedgerService ledgerService,
            IdempotencyService idempotencyService,
            TransactionLimitService transactionLimitService,
            TransactionQuotaService transactionQuotaService,
            OrganizationService organizationService,
            @Lazy SpendRequestService spendRequestService,
            ActivityLogService activityLogService) {
        this.messageConfig = messageConfig;
        this.walletRepository = walletRepository;
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
        this.walletRequestMapper = walletRequestMapper;
        this.walletResponseMapper = walletResponseMapper;
        this.walletTransactionRequestMapper = walletTransactionRequestMapper;
        this.ibanGenerator = ibanGenerator;
        this.securityAccess = securityAccess;
        this.paymentRail = paymentRail;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.transactionLimitService = transactionLimitService;
        this.transactionQuotaService = transactionQuotaService;
        this.organizationService = organizationService;
        this.spendRequestService = spendRequestService;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public WalletResponse findById(long id) {
        final Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOrgMemberOrAdmin(wallet);
        return walletResponseMapper.toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse findByIban(String iban) {
        final Wallet wallet = walletRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOrgMemberOrAdmin(wallet);
        return walletResponseMapper.toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> findByUserId(long userId) {
        securityAccess.requireSelfOrAdmin(userId);
        return walletRepository.findByUserId(userId).stream()
                .map(walletResponseMapper::toWalletResponse)
                .toList();
    }

    public Wallet getByIban(String iban) {
        return walletRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<WalletResponse> findAll(Pageable pageable) {
        final Page<Wallet> wallets;
        if (securityAccess.isAdmin()) {
            wallets = walletRepository.findAll(pageable);
        } else {
            Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
            wallets = walletRepository.findByOrganizationId(orgId, pageable);
        }
        if (wallets.isEmpty()) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        }
        return wallets.map(walletResponseMapper::toWalletResponse);
    }

    /**
     * Creates a new wallet in the active organization.
     */
    @Transactional
    public CommandResponse create(WalletRequest request) {
        final Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(orgId,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);

        final Long creatorId = securityAccess.currentUser().getId();
        request.setUserId(creatorId);
        request.setIban(generateUniqueIban());

        if (walletRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, request.getName())) {
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_NAME_EXISTS));
        }

        final Customer customer = resolveOwnerCustomer(orgId, request);

        final Wallet wallet = walletRequestMapper.toWallet(request);
        wallet.setOrganization(organizationService.getReferenceById(orgId));
        wallet.setOwnerType(request.getOwnerType());
        wallet.setCustomer(customer);
        walletRepository.save(wallet);
        log.info(messageConfig.getMessage(INFO_WALLET_CREATED, wallet.getId()));
        activityLogService.record(
                orgId,
                creatorId,
                ActivityEventType.WALLET_CREATE,
                "Wallet created",
                Map.of("walletId", wallet.getId(), "ownerType", request.getOwnerType().name()));

        TransactionRequest initialTx = walletTransactionRequestMapper.toTransactionRequest(request);
        Transaction transaction = transactionService.createEntity(initialTx);
        if (request.getBalance() != null && request.getBalance().signum() > 0) {
            ledgerService.postTopUp(transaction, wallet, request.getBalance(),
                    wallet.getCurrency() != null ? wallet.getCurrency() : CURRENCY_VND);
        }

        return CommandResponse.completed(wallet.getId());
    }

    /**
     * Resolves optional customer for owner label. Null when ORGANIZATION.
     */
    private Customer resolveOwnerCustomer(Long orgId, WalletRequest request) {
        WalletOwnerType ownerType = request.getOwnerType();
        if (ownerType == null) {
            throw new IllegalArgumentException(messageConfig.getMessage(ERROR_VALIDATION));
        }
        if (ownerType == WalletOwnerType.ORGANIZATION) {
            if (request.getCustomerId() != null) {
                throw new IllegalArgumentException(messageConfig.getMessage(ERROR_WALLET_OWNER_CUSTOMER_FORBIDDEN));
            }
            return null;
        }
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException(messageConfig.getMessage(ERROR_WALLET_OWNER_CUSTOMER_REQUIRED));
        }
        Customer customer = customerRepository.findByIdAndOrganizationId(request.getCustomerId(), orgId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_CUSTOMER_NOT_FOUND)));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalArgumentException(messageConfig.getMessage(ERROR_WALLET_OWNER_CUSTOMER_INACTIVE));
        }
        return customer;
    }

    private String generateUniqueIban() {
        for (int attempt = 0; attempt < IBAN_GENERATE_MAX_ATTEMPTS; attempt++) {
            final String iban = ibanGenerator.generate();
            if (!walletRepository.existsByIbanIgnoreCase(iban)) {
                return iban;
            }
        }
        throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_IBAN_EXISTS));
    }

    @Transactional
    public CommandResponse transferFunds(TransactionRequest request, String idempotencyKey) {
        final Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(orgId,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);

        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_TRANSFER, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        final Wallet fromWallet = getByIban(request.getFromWalletIban());
        securityAccess.requireWalletInActiveOrg(fromWallet);

        if (transactionLimitService.requiresDualControl(orgId, request.getAmount())) {
            transactionQuotaService.assertWithinQuota(orgId);
            return spendRequestService.createPending(orgId, OP_TRANSFER, request);
        }

        return executeTransfer(request, idempotencyKey, orgId);
    }

    /**
     * Executes a transfer immediately (used by dual-control approve and below-threshold path).
     */
    @Transactional
    public CommandResponse executeTransfer(TransactionRequest request, String idempotencyKey, Long orgId) {
        final Long userId = securityAccess.currentUser().getId();
        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_TRANSFER, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        final Wallet fromWallet = getByIbanForUpdate(request.getFromWalletIban());
        final Wallet toWallet = getByIban(request.getToWalletIban());
        if (fromWallet.getOrganization() == null || !orgId.equals(fromWallet.getOrganization().getId())) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND));
        }
        securityAccess.requireWalletOrgRole(fromWallet,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);
        transactionQuotaService.assertWithinQuota(orgId);
        transactionLimitService.assertTransferAllowed(orgId, request.getAmount());

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCES_UPDATED, fromWallet.getId(), toWallet.getId()));

        request.setTypeId(TYPE_TRANSFER);
        Transaction transaction = transactionService.createEntity(request);
        ledgerService.postTransfer(transaction, fromWallet, toWallet, request.getAmount(), currencyOf(fromWallet));

        idempotencyService.remember(orgId, userId, OP_TRANSFER, idempotencyKey, transaction.getId());
        return CommandResponse.completed(transaction.getId());
    }

    @Transactional
    public CommandResponse addFunds(TransactionRequest request, String idempotencyKey) {
        final Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(orgId,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);

        final Long userId = securityAccess.currentUser().getId();
        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_ADD_FUNDS, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        final Wallet toWallet = getByIbanForUpdate(request.getToWalletIban());
        securityAccess.requireWalletInActiveOrg(toWallet);
        transactionQuotaService.assertWithinQuota(orgId);
        transactionLimitService.assertTopUpAllowed(orgId, request.getAmount());

        RailResult railResult = paymentRail.initiateTopUp(
                toWallet.getIban(), request.getAmount(), request.getDescription());
        if (railResult.status() != Status.SUCCESS) {
            throw new IllegalStateException("Payment rail did not complete top-up: " + railResult.status());
        }

        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));
        walletRepository.save(toWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, toWallet.getId()));

        request.setTypeId(TYPE_TOP_UP);
        Transaction transaction = transactionService.createEntity(request);
        ledgerService.postTopUp(transaction, toWallet, request.getAmount(), currencyOf(toWallet));

        idempotencyService.remember(orgId, userId, OP_ADD_FUNDS, idempotencyKey, transaction.getId());
        return CommandResponse.completed(transaction.getId());
    }

    @Transactional
    public CommandResponse withdrawFunds(TransactionRequest request, String idempotencyKey) {
        final Long orgId = securityAccess.requireActiveOrganizationIdForMutation();
        securityAccess.requireOrgRole(orgId,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);

        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_WITHDRAW, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        final Wallet fromWallet = getByIban(request.getFromWalletIban());
        securityAccess.requireWalletInActiveOrg(fromWallet);

        if (transactionLimitService.requiresDualControl(orgId, request.getAmount())) {
            transactionQuotaService.assertWithinQuota(orgId);
            return spendRequestService.createPending(orgId, OP_WITHDRAW, request);
        }

        return executeWithdraw(request, idempotencyKey, orgId);
    }

    @Transactional
    public CommandResponse executeWithdraw(TransactionRequest request, String idempotencyKey, Long orgId) {
        final Long userId = securityAccess.currentUser().getId();
        Optional<Long> existing = idempotencyService.findResponseId(orgId, OP_WITHDRAW, idempotencyKey);
        if (existing.isPresent()) {
            return CommandResponse.completed(existing.get());
        }

        final Wallet fromWallet = getByIbanForUpdate(request.getFromWalletIban());
        if (fromWallet.getOrganization() == null || !orgId.equals(fromWallet.getOrganization().getId())) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND));
        }
        securityAccess.requireWalletOrgRole(fromWallet,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);
        transactionQuotaService.assertWithinQuota(orgId);
        transactionLimitService.assertWithdrawAllowed(orgId, request.getAmount());

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
        }

        RailResult railResult = paymentRail.initiateWithdraw(
                fromWallet.getIban(), request.getAmount(), request.getDescription());
        if (railResult.status() != Status.SUCCESS) {
            throw new IllegalStateException("Payment rail did not complete withdraw: " + railResult.status());
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(fromWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, fromWallet.getId()));

        request.setTypeId(TYPE_WITHDRAW);
        Transaction transaction = transactionService.createEntity(request);
        ledgerService.postWithdraw(transaction, fromWallet, request.getAmount(), currencyOf(fromWallet));

        idempotencyService.remember(orgId, userId, OP_WITHDRAW, idempotencyKey, transaction.getId());
        return CommandResponse.completed(transaction.getId());
    }

    @Transactional
    public CommandResponse update(long id, WalletRequest request) {
        final Wallet foundWallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOrgRole(foundWallet,
                OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.ACCOUNTANT);

        final Long orgId = foundWallet.getOrganization().getId();

        if (!request.getName().equalsIgnoreCase(foundWallet.getName())
                && walletRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, request.getName())) {
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_NAME_EXISTS));
        }

        foundWallet.setName(org.apache.commons.text.WordUtils.capitalizeFully(request.getName()));
        walletRepository.save(foundWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_UPDATED, foundWallet.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.WALLET_UPDATE,
                "Wallet renamed",
                Map.of("walletId", foundWallet.getId()));
        return CommandResponse.completed(id);
    }

    public Wallet getByIbanForUpdate(String iban) {
        return walletRepository.findByIbanForUpdate(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
    }

    @Transactional
    public void deleteById(long id) {
        final Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOrgRole(wallet, OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Long orgId = wallet.getOrganization().getId();
        walletRepository.delete(wallet);
        log.info(messageConfig.getMessage(INFO_WALLET_DELETED, wallet.getId()));
        activityLogService.record(
                orgId,
                securityAccess.currentUser().getId(),
                ActivityEventType.WALLET_DELETE,
                "Wallet deleted",
                Map.of("walletId", id));
    }

    private static String currencyOf(Wallet wallet) {
        return wallet.getCurrency() != null ? wallet.getCurrency() : CURRENCY_VND;
    }
}

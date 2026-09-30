package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.exception.TransactionLimitExceededException;
import com.ros.ewallet.repository.LedgerEntryRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_DAILY_OUTBOUND;
import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_DAILY_TOPUP;
import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_PER_TRANSACTION;
import static com.ros.ewallet.common.MessageKeys.ERROR_ORG_NOT_FOUND;

/**
 * Enforces per-transaction and daily money-mutation limits (VND, Asia/Ho_Chi_Minh day),
 * using each organization's stored limits.
 */
@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    static final ZoneId LIMIT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LedgerEntryRepository ledgerEntryRepository;
    private final OrganizationRepository organizationRepository;
    private final MessageSourceConfig messageConfig;
    private final Clock clock;

    public void assertTransferAllowed(Long organizationId, BigDecimal amount) {
        OrganizationLimits limits = resolveLimits(organizationId);
        assertPerTransaction(amount, limits.perTransactionMax());
        assertDailyOutbound(organizationId, amount, limits.dailyOutboundMax());
    }

    public void assertWithdrawAllowed(Long organizationId, BigDecimal amount) {
        OrganizationLimits limits = resolveLimits(organizationId);
        assertPerTransaction(amount, limits.perTransactionMax());
        assertDailyOutbound(organizationId, amount, limits.dailyOutboundMax());
    }

    public void assertTopUpAllowed(Long organizationId, BigDecimal amount) {
        OrganizationLimits limits = resolveLimits(organizationId);
        assertPerTransaction(amount, limits.perTransactionMax());
        assertDailyTopUp(organizationId, amount, limits.dailyTopUpMax());
    }

    public boolean requiresDualControl(Long organizationId, BigDecimal amount) {
        OrganizationLimits limits = resolveLimits(organizationId);
        BigDecimal threshold = limits.dualControlThreshold();
        return threshold != null && amount.compareTo(threshold) >= 0;
    }

    private void assertPerTransaction(BigDecimal amount, BigDecimal perTransactionMax) {
        if (amount.compareTo(perTransactionMax) > 0) {
            throw new TransactionLimitExceededException(messageConfig.getMessage(ERROR_LIMIT_PER_TRANSACTION));
        }
    }

    private void assertDailyOutbound(Long organizationId, BigDecimal amount, BigDecimal dailyOutboundMax) {
        Instant[] window = todayWindow();
        BigDecimal spent = nullToZero(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(
                organizationId, window[0], window[1]));
        if (spent.add(amount).compareTo(dailyOutboundMax) > 0) {
            throw new TransactionLimitExceededException(messageConfig.getMessage(ERROR_LIMIT_DAILY_OUTBOUND));
        }
    }

    private void assertDailyTopUp(Long organizationId, BigDecimal amount, BigDecimal dailyTopUpMax) {
        Instant[] window = todayWindow();
        BigDecimal toppedUp = nullToZero(ledgerEntryRepository.sumTopUpsForOrganizationBetween(
                organizationId, window[0], window[1]));
        if (toppedUp.add(amount).compareTo(dailyTopUpMax) > 0) {
            throw new TransactionLimitExceededException(messageConfig.getMessage(ERROR_LIMIT_DAILY_TOPUP));
        }
    }

    private OrganizationLimits resolveLimits(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));
        return new OrganizationLimits(
                org.getPerTransactionMax(),
                org.getDailyOutboundMax(),
                org.getDailyTopUpMax(),
                org.getDualControlThreshold());
    }

    private Instant[] todayWindow() {
        LocalDate today = Instant.now(clock).atZone(LIMIT_ZONE).toLocalDate();
        Instant start = today.atStartOfDay(LIMIT_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(LIMIT_ZONE).toInstant();
        return new Instant[]{start, end};
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record OrganizationLimits(
            BigDecimal perTransactionMax,
            BigDecimal dailyOutboundMax,
            BigDecimal dailyTopUpMax,
            BigDecimal dualControlThreshold) {
    }
}

package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.domain.enums.CustomerStatus;
import com.ros.ewallet.domain.enums.SpendRequestStatus;
import com.ros.ewallet.dto.response.OrganizationStatsResponse;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.repository.CustomerRepository;
import com.ros.ewallet.repository.LedgerEntryRepository;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.SpendRequestRepository;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.repository.WalletRepository;
import com.ros.ewallet.security.SecurityAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.ros.ewallet.common.MessageKeys.ERROR_ORG_NOT_FOUND;

/**
 * Aggregates org-scoped Dashboard stats from ledger entries and related counters.
 */
@Service
@RequiredArgsConstructor
public class OrganizationStatsService {

    static final ZoneId STATS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CURRENCY = "VND";

    private final OrganizationRepository organizationRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletRepository walletRepository;
    private final SpendRequestRepository spendRequestRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityAccess securityAccess;
    private final MessageSourceConfig messageConfig;
    private final Clock clock;

    @Transactional(readOnly = true)
    public OrganizationStatsResponse getStats(long organizationId) {
        if (!securityAccess.isAdmin()) {
            securityAccess.requireMembership(organizationId);
        }
        if (!organizationRepository.existsById(organizationId)) {
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND));
        }

        Instant[] today = todayWindow();
        List<Wallet> wallets = walletRepository.findByOrganizationId(organizationId);
        BigDecimal totalBalance = wallets.stream()
                .map(Wallet::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrganizationStatsResponse.builder()
                .transferTotal(nullToZero(ledgerEntryRepository.sumTransfersForOrganization(organizationId)))
                .withdrawTotal(nullToZero(ledgerEntryRepository.sumWithdrawsForOrganization(organizationId)))
                .receiveTotal(nullToZero(ledgerEntryRepository.sumTopUpsForOrganization(organizationId)))
                .totalBalance(totalBalance)
                .walletCount(wallets.size())
                .transactionCount(transactionRepository.countByOrganizationId(organizationId))
                .pendingApprovals(spendRequestRepository.countByOrganizationIdAndStatus(
                        organizationId, SpendRequestStatus.PENDING))
                .customerCount(customerRepository.countByOrganizationIdAndStatus(
                        organizationId, CustomerStatus.ACTIVE))
                .todayOutboundTotal(nullToZero(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(
                        organizationId, today[0], today[1])))
                .todayTopUpTotal(nullToZero(ledgerEntryRepository.sumTopUpsForOrganizationBetween(
                        organizationId, today[0], today[1])))
                .currency(CURRENCY)
                .build();
    }

    private Instant[] todayWindow() {
        LocalDate today = Instant.now(clock).atZone(STATS_ZONE).toLocalDate();
        Instant start = today.atStartOfDay(STATS_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(STATS_ZONE).toInstant();
        return new Instant[]{start, end};
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

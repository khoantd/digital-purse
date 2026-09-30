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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationStatsServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant FIXED = LocalDate.of(2026, 9, 30).atStartOfDay(VN).toInstant();

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SpendRequestRepository spendRequestRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private MessageSourceConfig messageConfig;

    private OrganizationStatsService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED, ZoneOffset.UTC);
        service = new OrganizationStatsService(
                organizationRepository,
                ledgerEntryRepository,
                walletRepository,
                spendRequestRepository,
                customerRepository,
                transactionRepository,
                securityAccess,
                messageConfig,
                clock);
        lenient().when(messageConfig.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageConfig.getMessage(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getStats_returnsZerosWhenOrgHasNoActivity() {
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(organizationRepository.existsById(10L)).thenReturn(true);
        when(ledgerEntryRepository.sumTransfersForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumWithdrawsForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumTopUpsForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumTopUpsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(walletRepository.findByOrganizationId(10L)).thenReturn(List.of());
        when(spendRequestRepository.countByOrganizationIdAndStatus(10L, SpendRequestStatus.PENDING))
                .thenReturn(0L);
        when(customerRepository.countByOrganizationIdAndStatus(10L, CustomerStatus.ACTIVE)).thenReturn(0L);
        when(transactionRepository.countByOrganizationId(10L)).thenReturn(0L);

        OrganizationStatsResponse stats = service.getStats(10L);

        assertEquals(0, stats.getTransferTotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.getWithdrawTotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.getReceiveTotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.getWalletCount());
        assertEquals(0, stats.getTotalBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0L, stats.getPendingApprovals());
        assertEquals(0L, stats.getCustomerCount());
        assertEquals(0L, stats.getTransactionCount());
        assertEquals("VND", stats.getCurrency());
    }

    @Test
    void getStats_aggregatesLedgerTotalsAndOpsCounters() {
        when(securityAccess.isAdmin()).thenReturn(false);
        when(securityAccess.requireMembership(10L)).thenReturn(null);
        when(organizationRepository.existsById(10L)).thenReturn(true);
        when(ledgerEntryRepository.sumTransfersForOrganization(10L)).thenReturn(new BigDecimal("1500000"));
        when(ledgerEntryRepository.sumWithdrawsForOrganization(10L)).thenReturn(new BigDecimal("500000"));
        when(ledgerEntryRepository.sumTopUpsForOrganization(10L)).thenReturn(new BigDecimal("3000000"));
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(new BigDecimal("200000"));
        when(ledgerEntryRepository.sumTopUpsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(new BigDecimal("100000"));

        Wallet w1 = wallet(1L, new BigDecimal("2000000"));
        Wallet w2 = wallet(2L, new BigDecimal("500000"));
        when(walletRepository.findByOrganizationId(10L)).thenReturn(List.of(w1, w2));
        when(spendRequestRepository.countByOrganizationIdAndStatus(10L, SpendRequestStatus.PENDING))
                .thenReturn(2L);
        when(customerRepository.countByOrganizationIdAndStatus(10L, CustomerStatus.ACTIVE)).thenReturn(5L);
        when(transactionRepository.countByOrganizationId(10L)).thenReturn(12L);

        OrganizationStatsResponse stats = service.getStats(10L);

        assertEquals(0, stats.getTransferTotal().compareTo(new BigDecimal("1500000")));
        assertEquals(0, stats.getWithdrawTotal().compareTo(new BigDecimal("500000")));
        assertEquals(0, stats.getReceiveTotal().compareTo(new BigDecimal("3000000")));
        assertEquals(2, stats.getWalletCount());
        assertEquals(0, stats.getTotalBalance().compareTo(new BigDecimal("2500000")));
        assertEquals(2L, stats.getPendingApprovals());
        assertEquals(5L, stats.getCustomerCount());
        assertEquals(12L, stats.getTransactionCount());
        assertEquals(0, stats.getTodayOutboundTotal().compareTo(new BigDecimal("200000")));
        assertEquals(0, stats.getTodayTopUpTotal().compareTo(new BigDecimal("100000")));
    }

    @Test
    void getStats_allowsAdminWithoutMembership() {
        when(securityAccess.isAdmin()).thenReturn(true);
        when(organizationRepository.existsById(10L)).thenReturn(true);
        when(ledgerEntryRepository.sumTransfersForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumWithdrawsForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumTopUpsForOrganization(10L)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumTopUpsForOrganizationBetween(eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(walletRepository.findByOrganizationId(10L)).thenReturn(List.of());
        when(spendRequestRepository.countByOrganizationIdAndStatus(10L, SpendRequestStatus.PENDING))
                .thenReturn(0L);
        when(customerRepository.countByOrganizationIdAndStatus(10L, CustomerStatus.ACTIVE)).thenReturn(0L);
        when(transactionRepository.countByOrganizationId(10L)).thenReturn(0L);

        service.getStats(10L);

        verify(securityAccess).isAdmin();
    }

    @Test
    void getStats_throwsWhenOrganizationMissing() {
        when(securityAccess.isAdmin()).thenReturn(true);
        when(organizationRepository.existsById(99L)).thenReturn(false);

        assertThrows(NoSuchElementFoundException.class, () -> service.getStats(99L));
    }

    private static Wallet wallet(Long id, BigDecimal balance) {
        Wallet w = new Wallet();
        w.setId(id);
        w.setBalance(balance);
        return w;
    }
}

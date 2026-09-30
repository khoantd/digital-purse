package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.enums.OrganizationStatus;
import com.ros.ewallet.exception.TransactionLimitExceededException;
import com.ros.ewallet.repository.LedgerEntryRepository;
import com.ros.ewallet.repository.OrganizationRepository;
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
import java.util.Optional;

import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_DAILY_OUTBOUND;
import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_DAILY_TOPUP;
import static com.ros.ewallet.common.MessageKeys.ERROR_LIMIT_PER_TRANSACTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLimitServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant FIXED = LocalDate.of(2026, 9, 30).atStartOfDay(VN).toInstant();

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MessageSourceConfig messageConfig;

    private TransactionLimitService limitService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED, ZoneOffset.UTC);
        limitService = new TransactionLimitService(
                ledgerEntryRepository, organizationRepository, messageConfig, clock);
        lenient().when(organizationRepository.findById(1L)).thenReturn(Optional.of(orgWithDefaults(1L)));
    }

    @Test
    void assertTransferAllowed_passesWhenUnderLimits() {
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("1000000"));

        assertDoesNotThrow(() ->
                limitService.assertTransferAllowed(1L, new BigDecimal("200000")));
    }

    @Test
    void assertTransferAllowed_rejectsWhenAmountExceedsPerTransactionMax() {
        when(messageConfig.getMessage(ERROR_LIMIT_PER_TRANSACTION)).thenReturn("per-tx");

        assertThrows(TransactionLimitExceededException.class, () ->
                limitService.assertTransferAllowed(1L, new BigDecimal("50000001")));

        verify(ledgerEntryRepository, never()).sumWalletDebitsForOrganizationBetween(any(), any(), any());
    }

    @Test
    void assertTransferAllowed_rejectsWhenDailyOutboundWouldExceed() {
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("99000000"));
        when(messageConfig.getMessage(ERROR_LIMIT_DAILY_OUTBOUND)).thenReturn("daily-out");

        assertThrows(TransactionLimitExceededException.class, () ->
                limitService.assertTransferAllowed(1L, new BigDecimal("2000000")));
    }

    @Test
    void assertWithdrawAllowed_rejectsWhenDailyOutboundWouldExceed() {
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("100000000"));
        when(messageConfig.getMessage(ERROR_LIMIT_DAILY_OUTBOUND)).thenReturn("daily-out");

        assertThrows(TransactionLimitExceededException.class, () ->
                limitService.assertWithdrawAllowed(1L, new BigDecimal("1")));
    }

    @Test
    void assertTopUpAllowed_rejectsWhenDailyTopUpWouldExceed() {
        when(ledgerEntryRepository.sumTopUpsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("95000000"));
        when(messageConfig.getMessage(ERROR_LIMIT_DAILY_TOPUP)).thenReturn("daily-topup");

        assertThrows(TransactionLimitExceededException.class, () ->
                limitService.assertTopUpAllowed(1L, new BigDecimal("6000000")));
    }

    @Test
    void assertTopUpAllowed_passesWhenUnderLimits() {
        when(ledgerEntryRepository.sumTopUpsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() ->
                limitService.assertTopUpAllowed(1L, new BigDecimal("50000000")));
    }

    @Test
    void nullDailySum_treatedAsZero() {
        when(ledgerEntryRepository.sumWalletDebitsForOrganizationBetween(eq(1L), any(), any()))
                .thenReturn(null);

        assertDoesNotThrow(() ->
                limitService.assertTransferAllowed(1L, new BigDecimal("100")));
    }

    @Test
    void requiresDualControl_usesOrganizationThreshold() {
        Organization custom = orgWithDefaults(2L);
        custom.setDualControlThreshold(new BigDecimal("5000000"));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(custom));

        assertTrue(limitService.requiresDualControl(2L, new BigDecimal("5000000")));
        assertTrue(limitService.requiresDualControl(2L, new BigDecimal("5000001")));
        assertFalse(limitService.requiresDualControl(2L, new BigDecimal("4999999")));
    }

    @Test
    void assertTransferAllowed_usesOrganizationPerTransactionMax() {
        Organization custom = orgWithDefaults(3L);
        custom.setPerTransactionMax(new BigDecimal("1000000"));
        when(organizationRepository.findById(3L)).thenReturn(Optional.of(custom));
        when(messageConfig.getMessage(ERROR_LIMIT_PER_TRANSACTION)).thenReturn("per-tx");

        assertThrows(TransactionLimitExceededException.class, () ->
                limitService.assertTransferAllowed(3L, new BigDecimal("1000001")));
    }

    private static Organization orgWithDefaults(long id) {
        Organization org = new Organization();
        org.setId(id);
        org.setName("Test Org");
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setCreatedAt(Instant.now());
        org.setPerTransactionMax(new BigDecimal("50000000"));
        org.setDailyOutboundMax(new BigDecimal("100000000"));
        org.setDailyTopUpMax(new BigDecimal("100000000"));
        org.setDualControlThreshold(new BigDecimal("10000000"));
        return org;
    }
}

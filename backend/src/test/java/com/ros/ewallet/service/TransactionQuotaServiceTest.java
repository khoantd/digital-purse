package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.domain.enums.OrganizationStatus;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.exception.SubscriptionQuotaExceededException;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.ros.ewallet.common.MessageKeys.ERROR_ORG_NOT_FOUND;
import static com.ros.ewallet.common.MessageKeys.ERROR_SUBSCRIPTION_QUOTA_EXCEEDED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionQuotaServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MessageSourceConfig messageConfig;

    private TransactionQuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new TransactionQuotaService(
                organizationRepository, transactionRepository, messageConfig);
        lenient().when(messageConfig.getMessage(ERROR_SUBSCRIPTION_QUOTA_EXCEEDED)).thenReturn("quota");
        lenient().when(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)).thenReturn("not-found");
    }

    @Test
    void assertWithinQuota_passesWhenUnderQuota() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org(1L, 1000L)));
        when(transactionRepository.countByOrganizationId(1L)).thenReturn(999L);

        assertDoesNotThrow(() -> quotaService.assertWithinQuota(1L));
    }

    @Test
    void assertWithinQuota_rejectsWhenAtQuota() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org(1L, 1000L)));
        when(transactionRepository.countByOrganizationId(1L)).thenReturn(1000L);

        assertThrows(SubscriptionQuotaExceededException.class, () ->
                quotaService.assertWithinQuota(1L));
    }

    @Test
    void assertWithinQuota_rejectsWhenOverQuota() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org(1L, 100L)));
        when(transactionRepository.countByOrganizationId(1L)).thenReturn(150L);

        assertThrows(SubscriptionQuotaExceededException.class, () ->
                quotaService.assertWithinQuota(1L));
    }

    @Test
    void assertWithinQuota_rejectsMissingOrg() {
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementFoundException.class, () ->
                quotaService.assertWithinQuota(99L));
    }

    @Test
    void countUsed_delegatesToRepository() {
        when(transactionRepository.countByOrganizationId(1L)).thenReturn(42L);

        assertEquals(42L, quotaService.countUsed(1L));
        verify(transactionRepository).countByOrganizationId(1L);
    }

    private static Organization org(long id, long quota) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Test");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedAt(Instant.now());
        organization.setTransactionQuota(quota);
        return organization;
    }
}

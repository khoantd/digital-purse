package com.ros.ewallet.service;

import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.domain.entity.Organization;
import com.ros.ewallet.exception.NoSuchElementFoundException;
import com.ros.ewallet.exception.SubscriptionQuotaExceededException;
import com.ros.ewallet.repository.OrganizationRepository;
import com.ros.ewallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.ros.ewallet.common.MessageKeys.ERROR_ORG_NOT_FOUND;
import static com.ros.ewallet.common.MessageKeys.ERROR_SUBSCRIPTION_QUOTA_EXCEEDED;

/**
 * Enforces lifetime per-organization transaction subscription quota.
 */
@Service
@RequiredArgsConstructor
public class TransactionQuotaService {

    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final MessageSourceConfig messageConfig;

    public void assertWithinQuota(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_ORG_NOT_FOUND)));
        long used = transactionRepository.countByOrganizationId(organizationId);
        if (used >= org.getTransactionQuota()) {
            throw new SubscriptionQuotaExceededException(
                    messageConfig.getMessage(ERROR_SUBSCRIPTION_QUOTA_EXCEEDED));
        }
    }

    public long countUsed(Long organizationId) {
        return transactionRepository.countByOrganizationId(organizationId);
    }
}

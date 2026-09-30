package com.ros.ewallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Default subscription allotments for new organizations.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    /** Lifetime transaction quota assigned to newly created organizations. */
    private long defaultTransactionQuota = 1000L;
}

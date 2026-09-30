package com.ros.ewallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configurable transactional limits for Vietnam-first demo (amounts in VND).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.limits")
public class TransactionLimitProperties {

    /** Max amount for a single transfer, top-up, or withdraw. Default 50,000,000 VND. */
    private BigDecimal perTransactionMax = new BigDecimal("50000000");

    /** Max sum of outbound wallet debits (transfer + withdraw) per user per calendar day (Asia/Ho_Chi_Minh). */
    private BigDecimal dailyOutboundMax = new BigDecimal("100000000");

    /** Max sum of top-ups (SYSTEM_FLOAT → WALLET) per organization per calendar day (Asia/Ho_Chi_Minh). */
    private BigDecimal dailyTopUpMax = new BigDecimal("100000000");

    /**
     * Transfer/withdraw at or above this amount requires dual-control approval.
     * Default 10,000,000 VND.
     */
    private BigDecimal dualControlThreshold = new BigDecimal("10000000");

    /** How long after creation a SUCCESS transaction may be reversed. Default 72 hours. */
    private int reverseWindowHours = 72;
}

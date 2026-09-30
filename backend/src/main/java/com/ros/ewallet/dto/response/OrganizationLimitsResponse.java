package com.ros.ewallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Organization transactional limits (VND) for Settings UI and enforcement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLimitsResponse {

    private BigDecimal perTransactionMax;
    private BigDecimal dailyOutboundMax;
    private BigDecimal dailyTopUpMax;
    private BigDecimal dualControlThreshold;
    private String currency;
}

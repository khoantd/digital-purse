package com.ros.ewallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * OWNER/ADMIN update of organization transactional limits (VND whole đồng).
 */
@Data
public class OrganizationLimitsRequest {

    @NotNull(message = "{validation.field.amount.required}")
    @DecimalMin(value = "1", message = "{validation.field.amount.positive}")
    @Digits(integer = 19, fraction = 0, message = "{validation.field.amount.digits}")
    private BigDecimal perTransactionMax;

    @NotNull(message = "{validation.field.amount.required}")
    @DecimalMin(value = "1", message = "{validation.field.amount.positive}")
    @Digits(integer = 19, fraction = 0, message = "{validation.field.amount.digits}")
    private BigDecimal dailyOutboundMax;

    @NotNull(message = "{validation.field.amount.required}")
    @DecimalMin(value = "1", message = "{validation.field.amount.positive}")
    @Digits(integer = 19, fraction = 0, message = "{validation.field.amount.digits}")
    private BigDecimal dailyTopUpMax;

    @NotNull(message = "{validation.field.amount.required}")
    @DecimalMin(value = "1", message = "{validation.field.amount.positive}")
    @Digits(integer = 19, fraction = 0, message = "{validation.field.amount.digits}")
    private BigDecimal dualControlThreshold;
}

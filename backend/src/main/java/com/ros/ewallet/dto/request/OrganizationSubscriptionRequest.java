package com.ros.ewallet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganizationSubscriptionRequest {

    @NotNull
    @Min(1)
    private Long transactionQuota;
}

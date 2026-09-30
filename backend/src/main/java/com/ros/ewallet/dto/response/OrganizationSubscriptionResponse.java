package com.ros.ewallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSubscriptionResponse {

    private long transactionQuota;
    private long transactionUsed;
    private long remaining;
}

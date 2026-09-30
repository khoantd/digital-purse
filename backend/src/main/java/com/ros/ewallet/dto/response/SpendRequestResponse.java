package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.SpendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendRequestResponse {

    private Long id;
    private Long organizationId;
    private Long requesterId;
    private String requesterUsername;
    private Long approverId;
    private String operation;
    private BigDecimal amount;
    private String description;
    private String fromWalletIban;
    private String toWalletIban;
    private SpendRequestStatus status;
    private Long transactionId;
    private Long sourceTransactionId;
    private Instant createdAt;
    private Instant resolvedAt;
}

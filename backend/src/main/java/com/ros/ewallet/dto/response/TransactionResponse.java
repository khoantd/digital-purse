package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for Transaction response.
 */
@Data
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String description;
    private String createdAt;
    private UUID referenceNumber;
    private Status status;
    private WalletResponse fromWallet;
    private WalletResponse toWallet;
    private TypeResponse type;
    /** Present when this row is a reverse of another transaction. */
    private Long reversesTransactionId;
    /** Present when another transaction has reversed this row. */
    private Long reversedByTransactionId;
    /** Whether this SUCCESS money movement is eligible for reverse (server rules; role still enforced). */
    private Boolean reversible;
}

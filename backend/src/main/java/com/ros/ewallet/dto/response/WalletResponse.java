package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.WalletOwnerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Wallet response.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponse {

    private Long id;
    private String iban;
    private String name;
    private BigDecimal balance;
    private String currency;
    private WalletOwnerType ownerType;
    private Long customerId;
    private String customerName;
    /** Static VietQR EMVCo payload for receive screen (demo rail). */
    private String vietQrPayload;
    private UserResponse user;
}

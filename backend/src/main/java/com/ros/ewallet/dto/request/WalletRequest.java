package com.ros.ewallet.dto.request;

import com.ros.ewallet.domain.enums.WalletOwnerType;
import com.ros.ewallet.validator.ValidIban;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Wallet request.
 * Server-controlled fields (id) are omitted; userId is derived from the auth principal in the service.
 * On create, iban is generated server-side and any client value is ignored.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletRequest {

    /**
     * Optional on create (server generates). On update, when provided must be a valid IBAN.
     */
    @ValidIban(message = "{validation.iban.format}")
    private String iban;

    @Size(min = 3, max = 50, message = "{validation.field.name.length}")
    @NotBlank(message = "{validation.field.name.required}")
    private String name;

    @NotNull(message = "{validation.field.balance.required}")
    @Positive(message = "{validation.field.balance.positive}")
    @Digits(integer = 12, fraction = 2, message = "{validation.field.balance.digits}")
    private BigDecimal balance;

    /**
     * Required on create: ORGANIZATION (no customerId) or CUSTOMER (customerId required).
     */
    @NotNull(message = "{validation.field.ownerType.required}")
    private WalletOwnerType ownerType;

    /**
     * Required when ownerType is CUSTOMER; must be an ACTIVE customer in the active org.
     * Must be null when ownerType is ORGANIZATION.
     */
    private Long customerId;

    /**
     * Set server-side from the authenticated principal; ignored if supplied by the client.
     */
    private Long userId;
}

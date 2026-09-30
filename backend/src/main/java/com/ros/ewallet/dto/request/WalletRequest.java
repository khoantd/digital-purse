package com.ros.ewallet.dto.request;

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
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletRequest {

    @ValidIban(message = "{validation.iban.format}")
    @NotBlank(message = "{validation.iban.required}")
    private String iban;

    @Size(min = 3, max = 50, message = "{validation.field.name.length}")
    @NotBlank(message = "{validation.field.name.required}")
    private String name;

    @NotNull(message = "{validation.field.balance.required}")
    @Positive(message = "{validation.field.balance.positive}")
    @Digits(integer = 12, fraction = 2, message = "{validation.field.balance.digits}")
    private BigDecimal balance;

    /**
     * Set server-side from the authenticated principal; ignored if supplied by the client.
     */
    private Long userId;
}

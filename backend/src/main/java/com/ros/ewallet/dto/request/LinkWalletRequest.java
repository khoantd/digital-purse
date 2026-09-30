package com.ros.ewallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkWalletRequest {

    @NotBlank(message = "{validation.iban.required}")
    private String iban;
}

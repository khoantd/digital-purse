package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String taxId;
    private String notes;
    private CustomerStatus status;
    private Long linkedWalletId;
    private String linkedWalletIban;
    private String linkedWalletName;
}

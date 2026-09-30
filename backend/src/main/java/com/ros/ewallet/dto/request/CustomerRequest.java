package com.ros.ewallet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "{validation.field.name.required}")
    @Size(min = 2, max = 100, message = "{validation.field.name.length}")
    private String name;

    @Size(max = 20)
    private String phone;

    @Email(message = "{validation.user.email.format}")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String taxId;

    @Size(max = 500)
    private String notes;
}

package com.ros.ewallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationRequest {

    @NotBlank(message = "{validation.field.name.required}")
    @Size(min = 2, max = 100, message = "{validation.field.name.length}")
    private String name;

    @Size(max = 20)
    private String taxId;
}

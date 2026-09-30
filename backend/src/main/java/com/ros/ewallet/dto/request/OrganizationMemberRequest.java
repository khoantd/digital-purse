package com.ros.ewallet.dto.request;

import com.ros.ewallet.domain.enums.OrganizationRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationMemberRequest {

    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotNull
    private OrganizationRole role;
}

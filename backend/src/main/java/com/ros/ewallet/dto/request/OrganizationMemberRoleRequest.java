package com.ros.ewallet.dto.request;

import com.ros.ewallet.domain.enums.OrganizationRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganizationMemberRoleRequest {

    @NotNull
    private OrganizationRole role;
}

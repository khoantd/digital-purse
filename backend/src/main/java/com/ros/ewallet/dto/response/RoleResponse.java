package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.RoleType;
import lombok.Data;

/**
 * Data Transfer Object for Role response.
 */
@Data
public class RoleResponse {

    private Long id;
    private RoleType type;
}

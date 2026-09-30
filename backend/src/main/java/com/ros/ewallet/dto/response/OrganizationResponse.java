package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.OrganizationRole;
import com.ros.ewallet.domain.enums.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private Long id;
    private String name;
    private String taxId;
    private OrganizationStatus status;
    /** Caller's role in this org (null for platform admin listing without membership). */
    private OrganizationRole myRole;
}

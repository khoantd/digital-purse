package com.ros.ewallet.dto.response;

import com.ros.ewallet.domain.enums.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {

    private Long id;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private OrganizationRole role;
}

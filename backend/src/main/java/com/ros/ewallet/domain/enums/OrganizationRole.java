package com.ros.ewallet.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Role of a user inside an Organization (SME tenant). Separate from platform {@link RoleType}.
 */
@Getter
@AllArgsConstructor
public enum OrganizationRole {

    OWNER("Owner"),
    ADMIN("Admin"),
    ACCOUNTANT("Accountant"),
    APPROVER("Approver");

    private final String label;

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canMutateMoney() {
        return this == OWNER || this == ADMIN || this == ACCOUNTANT;
    }

    /** Create / update / archive / link customers (payee directory). */
    public boolean canManageCustomers() {
        return this == OWNER || this == ADMIN || this == ACCOUNTANT;
    }

    public boolean canApproveSpend() {
        return this == OWNER || this == ADMIN || this == APPROVER;
    }

    public boolean canDeleteOrg() {
        return this == OWNER;
    }
}

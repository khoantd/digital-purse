package com.ros.ewallet.domain.enums;

/**
 * Label for which entity a wallet is associated with.
 * Tenancy remains {@code organization_id}; this is classification metadata, not B2B2C sub-accounts.
 */
public enum WalletOwnerType {
    ORGANIZATION,
    CUSTOMER
}

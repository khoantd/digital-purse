package com.ros.ewallet.domain.enums;

import java.util.Set;

/**
 * Security and admin activity events (not money transfer / top-up / withdraw).
 */
public final class ActivityEventType {

    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_SIGNUP = "AUTH_SIGNUP";

    public static final String ORG_CREATE = "ORG_CREATE";
    public static final String ORG_UPDATE = "ORG_UPDATE";
    public static final String ORG_MEMBER_ADD = "ORG_MEMBER_ADD";
    public static final String ORG_MEMBER_ROLE_UPDATE = "ORG_MEMBER_ROLE_UPDATE";
    public static final String ORG_MEMBER_REMOVE = "ORG_MEMBER_REMOVE";
    public static final String ORG_LIMITS_UPDATE = "ORG_LIMITS_UPDATE";

    public static final String CUSTOMER_CREATE = "CUSTOMER_CREATE";
    public static final String CUSTOMER_UPDATE = "CUSTOMER_UPDATE";
    public static final String CUSTOMER_ARCHIVE = "CUSTOMER_ARCHIVE";
    public static final String CUSTOMER_LINK_WALLET = "CUSTOMER_LINK_WALLET";
    public static final String CUSTOMER_UNLINK_WALLET = "CUSTOMER_UNLINK_WALLET";

    public static final String WALLET_CREATE = "WALLET_CREATE";
    public static final String WALLET_UPDATE = "WALLET_UPDATE";
    public static final String WALLET_DELETE = "WALLET_DELETE";

    public static final String SPEND_REQUEST_CREATE = "SPEND_REQUEST_CREATE";
    public static final String SPEND_APPROVE = "SPEND_APPROVE";
    public static final String SPEND_REJECT = "SPEND_REJECT";

    public static final String TX_REVERSE = "TX_REVERSE";

    public static final Set<String> AUTH_TYPES = Set.of(AUTH_LOGIN, AUTH_LOGOUT, AUTH_SIGNUP);

    private ActivityEventType() {
    }
}

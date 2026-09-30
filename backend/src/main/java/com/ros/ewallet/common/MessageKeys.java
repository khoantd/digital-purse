package com.ros.ewallet.common;

/**
 * Keeps all message keys in one place.
 */
public final class MessageKeys {

    private MessageKeys() {
        throw new UnsupportedOperationException();
    }

    // info messages
    public static final String INFO_USER_LOGIN = "info.user.login";
    public static final String INFO_USER_CREATED = "info.user.created";
    public static final String INFO_WALLET_CREATED = "info.wallet.created";
    public static final String INFO_TRANSACTION_CREATED = "info.transaction.created";
    public static final String INFO_WALLET_UPDATED = "info.wallet.updated";
    public static final String INFO_WALLET_DELETED = "info.wallet.deleted";
    public static final String INFO_WALLET_BALANCE_UPDATED = "info.wallet.balance.updated";
    public static final String INFO_WALLET_BALANCES_UPDATED = "info.wallet.balances.updated";

    // exception messages
    public static final String ERROR_UNKNOWN = "error.unknown";
    public static final String ERROR_UNAUTHORIZED = "error.unauthorized";
    public static final String ERROR_UNAUTHORIZED_DETAILS = "error.unauthorized.details";
    public static final String ERROR_FORBIDDEN = "error.forbidden";
    public static final String ERROR_AUTH_SETUP = "error.auth.setup";
    public static final String ERROR_USERNAME_NOT_FOUND = "error.username.not.found";
    public static final String ERROR_JWT_INVALID_SIGNATURE = "error.jwt.invalid.signature";
    public static final String ERROR_JWT_INVALID_TOKEN = "error.jwt.invalid.token";
    public static final String ERROR_JWT_EXPIRED = "error.jwt.expired";
    public static final String ERROR_JWT_UNSUPPORTED = "error.jwt.unsupported";
    public static final String ERROR_JWT_EMPTY_CLAIMS = "error.jwt.empty.claims";
    public static final String ERROR_VALIDATION = "error.validation";
    public static final String ERROR_METHOD_ARGUMENT = "error.method.argument";
    public static final String ERROR_FIELD_VALIDATION = "error.field.validation";
    public static final String ERROR_ALREADY_EXISTS = "error.already.exists";
    public static final String ERROR_USERNAME_EXISTS = "error.username.exists";
    public static final String ERROR_EMAIL_EXISTS = "error.email.exists";
    public static final String ERROR_CREDENTIALS_IN_USE = "error.credentials.in.use";
    public static final String ERROR_TOO_MANY_REQUESTS = "error.too.many.requests";
    public static final String ERROR_WALLET_IBAN_EXISTS = "error.wallet.iban.exists";
    public static final String ERROR_WALLET_NAME_EXISTS = "error.wallet.name.exists";
    public static final String ERROR_NOT_FOUND = "error.not.found";
    public static final String ERROR_NO_RECORDS = "error.no.records";
    public static final String ERROR_WALLET_NOT_FOUND = "error.wallet.not.found";
    public static final String ERROR_TRANSACTION_NOT_FOUND = "error.transaction.not.found";
    public static final String ERROR_INSUFFICIENT_FUNDS = "error.insufficient.funds";
    public static final String ERROR_LIMIT_EXCEEDED = "error.limit.exceeded";
    public static final String ERROR_LIMIT_PER_TRANSACTION = "error.limit.per.transaction";
    public static final String ERROR_LIMIT_DAILY_OUTBOUND = "error.limit.daily.outbound";
    public static final String ERROR_LIMIT_DAILY_TOPUP = "error.limit.daily.topup";
    public static final String ERROR_LIMIT_DUAL_CONTROL_THRESHOLD = "error.limit.dual.control.threshold";
    public static final String ERROR_SUBSCRIPTION_QUOTA_EXCEEDED = "error.subscription.quota.exceeded";
    public static final String ERROR_SUBSCRIPTION_QUOTA_BELOW_USED = "error.subscription.quota.below.used";
    public static final String ERROR_ORG_REQUIRED = "error.org.required";
    public static final String ERROR_ORG_FORBIDDEN = "error.org.forbidden";
    public static final String ERROR_ORG_NOT_FOUND = "error.org.not.found";
    public static final String ERROR_MEMBERSHIP_EXISTS = "error.membership.exists";
    public static final String ERROR_MEMBERSHIP_NOT_FOUND = "error.membership.not.found";
    public static final String ERROR_LAST_OWNER = "error.membership.last.owner";
    public static final String ERROR_SPEND_NOT_FOUND = "error.spend.not.found";
    public static final String ERROR_SPEND_NOT_PENDING = "error.spend.not.pending";
    public static final String ERROR_SPEND_SELF_APPROVE = "error.spend.self.approve";
    public static final String INFO_ORG_CREATED = "info.org.created";
    public static final String INFO_ORG_UPDATED = "info.org.updated";
    public static final String INFO_ORG_LIMITS_UPDATED = "info.org.limits.updated";
    public static final String INFO_ORG_SUBSCRIPTION_UPDATED = "info.org.subscription.updated";
    public static final String INFO_MEMBER_ADDED = "info.member.added";
    public static final String INFO_MEMBER_UPDATED = "info.member.updated";
    public static final String INFO_MEMBER_REMOVED = "info.member.removed";
    public static final String INFO_SPEND_CREATED = "info.spend.created";
    public static final String INFO_SPEND_APPROVED = "info.spend.approved";
    public static final String INFO_SPEND_REJECTED = "info.spend.rejected";
    public static final String INFO_CUSTOMER_CREATED = "info.customer.created";
    public static final String INFO_CUSTOMER_UPDATED = "info.customer.updated";
    public static final String INFO_CUSTOMER_ARCHIVED = "info.customer.archived";
    public static final String INFO_CUSTOMER_LINKED = "info.customer.linked";
    public static final String INFO_CUSTOMER_UNLINKED = "info.customer.unlinked";
    public static final String ERROR_CUSTOMER_NOT_FOUND = "error.customer.not.found";
    public static final String ERROR_CUSTOMER_WALLET_LINKED = "error.customer.wallet.linked";
    public static final String ERROR_WALLET_OWNER_CUSTOMER_REQUIRED = "error.wallet.owner.customer.required";
    public static final String ERROR_WALLET_OWNER_CUSTOMER_FORBIDDEN = "error.wallet.owner.customer.forbidden";
    public static final String ERROR_WALLET_OWNER_CUSTOMER_INACTIVE = "error.wallet.owner.customer.inactive";
    public static final String ERROR_REVERSE_NOT_ALLOWED = "error.reverse.not.allowed";
    public static final String ERROR_REVERSE_ALREADY = "error.reverse.already";
    public static final String ERROR_REVERSE_WINDOW = "error.reverse.window";
    public static final String ERROR_REVERSE_TYPE = "error.reverse.type";
    public static final String ERROR_REVERSE_STATUS = "error.reverse.status";
}

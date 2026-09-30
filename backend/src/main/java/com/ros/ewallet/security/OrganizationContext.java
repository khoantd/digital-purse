package com.ros.ewallet.security;

/**
 * Thread-local active organization for the current request ({@code X-Organization-Id}).
 */
public final class OrganizationContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static Long getOrganizationId() {
        return CURRENT.get();
    }

    public static void setOrganizationId(Long organizationId) {
        CURRENT.set(organizationId);
    }

    public static boolean hasOrganization() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}

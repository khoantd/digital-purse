package com.ros.ewallet.exception;

/**
 * Thrown when an organization has used its lifetime transaction subscription quota.
 */
public class SubscriptionQuotaExceededException extends RuntimeException {

    public SubscriptionQuotaExceededException(String message) {
        super(message);
    }
}

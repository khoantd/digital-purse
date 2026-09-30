package com.ros.ewallet.exception;

/**
 * Thrown when a money mutation exceeds configured per-transaction or daily limits.
 */
public class TransactionLimitExceededException extends RuntimeException {

    public TransactionLimitExceededException(String message) {
        super(message);
    }
}

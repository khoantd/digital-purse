package com.github.yildizmy.exception;

/**
 * Thrown when the authenticated user is not allowed to access or mutate a resource.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

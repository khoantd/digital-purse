package com.github.yildizmy.exception;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super();
    }

    public TooManyRequestsException(String message) {
        super(message);
    }

    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause);
    }
}

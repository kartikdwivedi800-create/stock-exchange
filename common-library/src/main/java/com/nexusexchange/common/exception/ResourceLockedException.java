package com.nexusexchange.common.exception;

/** The resource is temporarily locked (HTTP 423), e.g. an account locked after too many failed logins. */
public class ResourceLockedException extends RuntimeException {
    public ResourceLockedException(String message) {
        super(message);
    }

    public ResourceLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}

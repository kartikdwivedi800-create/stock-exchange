package com.nexusexchange.user.exception;

import com.nexusexchange.common.exception.UnauthorizedException;

/** Wrong username/email or password (HTTP 401 via the common handler). */
public class InvalidCredentialsException extends UnauthorizedException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

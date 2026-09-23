package com.nexusexchange.user.exception;

import com.nexusexchange.common.exception.ConflictException;

/** Username or email already registered (HTTP 409 via the common handler). */
public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

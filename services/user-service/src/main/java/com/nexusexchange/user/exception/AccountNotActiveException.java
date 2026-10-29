package com.nexusexchange.user.exception;

import com.nexusexchange.common.exception.ForbiddenException;

/** The account is SUSPENDED, INACTIVE or PENDING (HTTP 403 via the common handler). */
public class AccountNotActiveException extends ForbiddenException {
    public AccountNotActiveException() {
        super("Account is not active");
    }
}

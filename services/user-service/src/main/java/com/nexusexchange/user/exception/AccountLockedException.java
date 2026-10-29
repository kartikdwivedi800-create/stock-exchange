package com.nexusexchange.user.exception;

import com.nexusexchange.common.exception.ResourceLockedException;

/** The account is temporarily locked after too many failed logins (HTTP 423 via the common handler). */
public class AccountLockedException extends ResourceLockedException {
    public AccountLockedException() {
        super("Account temporarily locked");
    }
}

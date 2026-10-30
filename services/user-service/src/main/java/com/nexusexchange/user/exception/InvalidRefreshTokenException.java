package com.nexusexchange.user.exception;

import com.nexusexchange.common.exception.UnauthorizedException;

/**
 * The refresh token is unknown, revoked, reused or expired (HTTP 401 via the common handler).
 * One generic message on purpose, so a client cannot tell the cases apart.
 */
public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}

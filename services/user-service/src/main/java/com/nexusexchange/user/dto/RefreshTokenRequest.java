package com.nexusexchange.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /auth/refresh and POST /auth/logout. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {

    /** Never print the token, even by accident. */
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=****]";
    }
}

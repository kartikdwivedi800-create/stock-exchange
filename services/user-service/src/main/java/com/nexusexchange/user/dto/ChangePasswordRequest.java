package com.nexusexchange.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PUT /users/me/password. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {

    /** Never print passwords, even by accident. */
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}

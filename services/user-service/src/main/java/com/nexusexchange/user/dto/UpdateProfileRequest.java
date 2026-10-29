package com.nexusexchange.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /users/me. Deliberately has no username, email, role or status
 * so a client cannot change them (no mass assignment).
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Pattern(regexp = "^\\+?[0-9]{7,15}$") String phoneNumber) {
}

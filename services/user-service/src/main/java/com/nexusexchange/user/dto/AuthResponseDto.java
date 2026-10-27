package com.nexusexchange.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Access-token lifetime in seconds. */
    private long expiresIn;

    /** Filled in by NX-303; null until then. */
    private String refreshToken;

    private UserProfileDto user;
}

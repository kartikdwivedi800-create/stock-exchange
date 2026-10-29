package com.nexusexchange.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    @ToString.Exclude
    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Access-token lifetime in seconds. */
    private long expiresIn;

    /** Raw refresh token, returned only here. Opaque; only its hash is stored. */
    @ToString.Exclude
    private String refreshToken;

    private UserProfileDto user;
}

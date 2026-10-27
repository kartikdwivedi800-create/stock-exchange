package com.nexusexchange.user.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Binds the {@code nexus.security} block of application.yml. */
@ConfigurationProperties("nexus.security")
@Validated
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @NotNull Duration refreshTokenTtl,
        @Valid @NotNull Login login) {

    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String secret,
            @NotNull Duration accessTokenTtl) {

        /** Never print the secret, even by accident. */
        @Override
        public String toString() {
            return "Jwt[issuer=" + issuer + ", secret=****, accessTokenTtl=" + accessTokenTtl + "]";
        }
    }

    public record Login(
            @Positive int maxFailedAttempts,
            @NotNull Duration lockDuration) {
    }
}

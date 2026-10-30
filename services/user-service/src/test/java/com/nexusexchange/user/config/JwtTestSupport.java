package com.nexusexchange.user.config;

import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Clock;

/** Shared fixtures for JWT tests. */
public final class JwtTestSupport {

    public static final String ISSUER = "nexus-exchange";
    public static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private JwtTestSupport() {
    }

    public static SecurityProperties properties(String secret) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(ISSUER, secret, Duration.ofMinutes(15)),
                Duration.ofDays(7),
                new SecurityProperties.Login(5, Duration.ofMinutes(15)));
    }

    public static SecurityProperties properties() {
        return properties(SECRET);
    }

    public static JwtEncoder encoder() {
        return new JwtConfig().jwtEncoder(properties());
    }

    public static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}

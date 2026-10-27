package com.nexusexchange.user.service;

import com.nexusexchange.user.config.JwtConfig;
import com.nexusexchange.user.config.JwtTestSupport;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private final JwtDecoder lenientDecoder = new JwtConfig().jwtDecoder(JwtTestSupport.properties());

    private User user(Role role) {
        return User.builder().id(42L).username("kartik").role(role).build();
    }

    private TokenService serviceAt(Instant now) {
        return new TokenService(JwtTestSupport.encoder(), JwtTestSupport.properties(), JwtTestSupport.clockAt(now));
    }

    /** Issues a token with the real clock, so the production decoder (which checks exp) accepts it. */
    private Jwt decodeAtRealNow(User user) {
        TokenService service = new TokenService(JwtTestSupport.encoder(), JwtTestSupport.properties(),
                java.time.Clock.systemUTC());
        return lenientDecoder.decode(service.createAccessToken(user));
    }

    @Test
    void containsAllRequiredClaims() {
        Jwt jwt = decodeAtRealNow(user(Role.ROLE_USER));

        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtTestSupport.ISSUER);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("username")).isEqualTo("kartik");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ROLE_USER");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(UUID.fromString(jwt.getId())).isNotNull();
    }

    @Test
    void subjectIsTheIdAsString() {
        Jwt jwt = decodeAtRealNow(user(Role.ROLE_USER));

        assertThat(jwt.getClaims().get("sub")).isInstanceOf(String.class).isEqualTo("42");
    }

    @Test
    void expiryIs900SecondsAfterIssuedAt() {
        String token = serviceAt(NOW).createAccessToken(user(Role.ROLE_USER));
        Jwt jwt = decodeIgnoringTime(token);

        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofSeconds(900));
    }

    @Test
    void roleClaimMatchesUserRole() {
        Jwt admin = decodeAtRealNow(user(Role.ROLE_ADMIN));

        assertThat(admin.<String>getClaim("role")).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void eachTokenGetsAUniqueJti() {
        TokenService service = serviceAt(NOW);
        User u = user(Role.ROLE_USER);

        Jwt first = decodeIgnoringTime(service.createAccessToken(u));
        Jwt second = decodeIgnoringTime(service.createAccessToken(u));

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void expiresInIs900Seconds() {
        assertThat(serviceAt(NOW).accessTokenExpiresInSeconds()).isEqualTo(900L);
    }

    /** Parses the token without validating exp (fixed clock in the past), to inspect claims. */
    private Jwt decodeIgnoringTime(String token) {
        var decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withSecretKey(new javax.crypto.spec.SecretKeySpec(
                        JwtTestSupport.SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }
}

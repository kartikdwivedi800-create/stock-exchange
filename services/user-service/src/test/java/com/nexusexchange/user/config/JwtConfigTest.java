package com.nexusexchange.user.config;

import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();
    private final User user = User.builder().id(7L).username("kartik").role(Role.ROLE_USER).build();

    private String tokenIssuedBy(SecurityProperties props, Clock clock) {
        JwtEncoder encoder = config.jwtEncoder(props);
        return new TokenService(encoder, props, clock).createAccessToken(user);
    }

    private JwtDecoder decoder() {
        return config.jwtDecoder(JwtTestSupport.properties());
    }

    @Test
    void validTokenDecodes() {
        String token = tokenIssuedBy(JwtTestSupport.properties(), Clock.systemUTC());

        Jwt jwt = decoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ROLE_USER");
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = tokenIssuedBy(JwtTestSupport.properties(), Clock.systemUTC());
        int signatureStart = token.lastIndexOf('.') + 1;
        int index = signatureStart + 5; // a middle character: the last one only has partial bits
        char replacement = token.charAt(index) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, index) + replacement + token.substring(index + 1);

        assertThatThrownBy(() -> decoder().decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void wrongIssuerIsRejected() {
        SecurityProperties otherIssuer = new SecurityProperties(
                new SecurityProperties.Jwt("someone-else", JwtTestSupport.SECRET, java.time.Duration.ofMinutes(15)),
                java.time.Duration.ofDays(7), new SecurityProperties.Login(5, java.time.Duration.ofMinutes(15)));
        String token = tokenIssuedBy(otherIssuer, Clock.systemUTC());

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock anHourAgo = JwtTestSupport.clockAt(Instant.now().minus(1, ChronoUnit.HOURS));
        String token = tokenIssuedBy(JwtTestSupport.properties(), anHourAgo);

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        SecurityProperties other = JwtTestSupport.properties("another-secret-that-is-also-32-bytes-long!!");
        String token = tokenIssuedBy(other, Clock.systemUTC());

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRejectedByEncoderAndDecoder() {
        SecurityProperties shortSecret = JwtTestSupport.properties("too-short");

        assertThatThrownBy(() -> config.jwtEncoder(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> config.jwtDecoder(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void secretOfExactly32BytesIsAccepted() {
        SecurityProperties exact = JwtTestSupport.properties("a".repeat(32));

        assertThat(config.jwtEncoder(exact)).isNotNull();
        assertThat(config.jwtDecoder(exact)).isNotNull();
    }

    @Test
    void applicationFailsToStartWithShortSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfig.class)
                .withPropertyValues(
                        "nexus.security.jwt.issuer=nexus-exchange",
                        "nexus.security.jwt.secret=too-short",
                        "nexus.security.jwt.access-token-ttl=15m",
                        "nexus.security.refresh-token-ttl=7d",
                        "nexus.security.login.max-failed-attempts=5",
                        "nexus.security.login.lock-duration=15m")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("at least 32 bytes");
                });
    }

    @Test
    void applicationStartsWithValidSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtConfig.class)
                .withPropertyValues(
                        "nexus.security.jwt.issuer=nexus-exchange",
                        "nexus.security.jwt.secret=" + JwtTestSupport.SECRET,
                        "nexus.security.jwt.access-token-ttl=15m",
                        "nexus.security.refresh-token-ttl=7d",
                        "nexus.security.login.max-failed-attempts=5",
                        "nexus.security.login.lock-duration=15m")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                    assertThat(context.getBean(SecurityProperties.class).refreshTokenTtl())
                            .isEqualTo(java.time.Duration.ofDays(7));
                });
    }
}

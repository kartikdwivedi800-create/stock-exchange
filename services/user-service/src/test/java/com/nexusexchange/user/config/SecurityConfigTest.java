package com.nexusexchange.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** NX-301: the production encoder must be BCrypt with cost factor 12. */
class SecurityConfigTest {

    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void encoderIsBcryptCost12() {
        String hash = encoder.encode("Secret123!");

        assertThat(hash).matches("^\\$2[ab]\\$12\\$.{53}$");
        assertThat(encoder.matches("Secret123!", hash)).isTrue();
        assertThat(encoder.matches("secret123!", hash)).isFalse();
    }
}

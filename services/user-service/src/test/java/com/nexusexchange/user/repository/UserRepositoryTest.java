package com.nexusexchange.user.repository;

import com.nexusexchange.common.enums.UserStatus;
import com.nexusexchange.testsupport.PostgresRepositoryTest;
import com.nexusexchange.user.entity.RefreshToken;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** user_db: V1 + V2 migrations, entity mapping and constraints. */
@PostgresRepositoryTest
class UserRepositoryTest {

    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;

    private User newUser(String username, String email) {
        return User.builder().username(username).email(email).passwordHash("$2a$12$hash")
                .firstName("Kartik").lastName("D").build();
    }

    @Test
    void savesUserWithDefaults() {
        User saved = users.saveAndFlush(newUser("kartik", "kartik@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void lookupsIgnoreCase() {
        users.saveAndFlush(newUser("Kartik", "Kartik@Example.com"));

        assertThat(users.findByUsernameIgnoreCase("KARTIK")).isPresent();
        assertThat(users.existsByEmailIgnoreCase("kartik@example.com")).isTrue();
    }

    @Test
    void usernameIsUniqueRegardlessOfCase() {
        users.saveAndFlush(newUser("Kartik", "a@example.com"));

        assertThatThrownBy(() -> users.saveAndFlush(newUser("kartik", "b@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_users_username_lower");
    }

    @Test
    void versionIncrementsOnUpdate() {
        User saved = users.saveAndFlush(newUser("kartik", "kartik@example.com"));
        saved.setFirstName("K");
        User updated = users.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void storesRefreshTokenForUser() {
        User user = users.saveAndFlush(newUser("kartik", "kartik@example.com"));
        refreshTokens.saveAndFlush(RefreshToken.builder().user(user).tokenHash("sha256-abc")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build());

        RefreshToken found = refreshTokens.findByTokenHash("sha256-abc").orElseThrow();
        assertThat(found.isActive(Instant.now())).isTrue();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
    }
}

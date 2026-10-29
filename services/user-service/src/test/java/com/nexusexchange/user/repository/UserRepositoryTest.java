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

    @Test
    void findByTokenHash_unknownHashIsEmpty() {
        assertThat(refreshTokens.findByTokenHash("no-such-hash")).isEmpty();
    }

    @Test
    void revokeAllActive_revokesOnlyActiveTokensOfThatUser() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant earlier = now.minus(1, ChronoUnit.HOURS);
        Instant future = now.plus(7, ChronoUnit.DAYS);
        User kartik = users.saveAndFlush(newUser("kartik", "kartik@example.com"));
        User other = users.saveAndFlush(newUser("other", "other@example.com"));
        refreshTokens.save(RefreshToken.builder().user(kartik).tokenHash("k-active-1").expiresAt(future).build());
        refreshTokens.save(RefreshToken.builder().user(kartik).tokenHash("k-active-2").expiresAt(future).build());
        refreshTokens.save(RefreshToken.builder().user(kartik).tokenHash("k-already-revoked")
                .expiresAt(future).revokedAt(earlier).build());
        refreshTokens.save(RefreshToken.builder().user(other).tokenHash("o-active").expiresAt(future).build());
        refreshTokens.flush();

        int revoked = refreshTokens.revokeAllActiveByUserId(kartik.getId(), now);

        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokens.findByTokenHash("k-active-1").orElseThrow().getRevokedAt()).isEqualTo(now);
        assertThat(refreshTokens.findByTokenHash("k-active-2").orElseThrow().getRevokedAt()).isEqualTo(now);
        assertThat(refreshTokens.findByTokenHash("k-already-revoked").orElseThrow().getRevokedAt())
                .isEqualTo(earlier);
        assertThat(refreshTokens.findByTokenHash("o-active").orElseThrow().getRevokedAt()).isNull();
    }
}

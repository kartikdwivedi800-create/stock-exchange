package com.nexusexchange.user.service;

import com.nexusexchange.user.config.JwtTestSupport;
import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.entity.RefreshToken;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.exception.InvalidRefreshTokenException;
import com.nexusexchange.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every branch of rotation, reuse detection and logout, with a clock the test controls. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final String RAW = "raw-token-presented-by-client";

    @Mock
    private RefreshTokenRepository repository;

    private MutableClock clock;
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        TokenService tokenService = new TokenService(JwtTestSupport.encoder(), JwtTestSupport.properties(), clock);
        service = new RefreshTokenService(repository, tokenService, JwtTestSupport.properties(), clock);
        user = User.builder().id(7L).username("kartik").email("k@example.com")
                .passwordHash("hash").firstName("K").lastName("D").role(Role.ROLE_USER).build();
    }

    private RefreshToken storedToken(Instant expiresAt, Instant revokedAt) {
        return RefreshToken.builder().id(1L).user(user).tokenHash(RefreshTokenService.hash(RAW))
                .expiresAt(expiresAt).revokedAt(revokedAt).build();
    }

    // ---------- hash helper ----------

    @Test
    void hash_sameInputGivesSame64CharLowercaseHex() {
        String first = RefreshTokenService.hash("abc");

        assertThat(first).isEqualTo(RefreshTokenService.hash("abc"))
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
        assertThat(RefreshTokenService.hash("abd")).isNotEqualTo(first);
        // Known SHA-256 vector for "abc"
        assertThat(first).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void rawToken_is43CharBase64UrlAndUnique() {
        String a = RefreshTokenService.generateRawToken();
        String b = RefreshTokenService.generateRawToken();

        assertThat(a).hasSize(43).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(a).isNotEqualTo(b);
    }

    // ---------- issue ----------

    @Test
    void issue_storesHashNotRawTokenWithExpiry() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(START.plus(Duration.ofDays(7)));
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getRevokedAt()).isNull();
    }

    // ---------- refresh ----------

    @Test
    void refresh_unknownToken_throwsAndRevokesNothing() {
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).revokeAllActiveByUserId(anyLong(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void refresh_nullToken_throws() {
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_revokedToken_isReuseAndRevokesAllOfUsersTokens() {
        RefreshToken revoked = storedToken(START.plus(Duration.ofDays(1)), START.minusSeconds(60));
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeAllActiveByUserId(7L, START);
        verify(repository, never()).save(any());
    }

    @Test
    void refresh_expiredToken_throwsWithoutReuseHandling() {
        RefreshToken expired = storedToken(START.minusSeconds(1), null);
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(expired.getRevokedAt()).isNull();
        verify(repository, never()).revokeAllActiveByUserId(anyLong(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void refresh_tokenExpiringExactlyNow_isExpired() {
        RefreshToken edge = storedToken(START, null);
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(edge));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_validToken_rotatesAndReturnsNewPair() {
        RefreshToken old = storedToken(START.plus(Duration.ofDays(1)), null);
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(old));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        clock.set(START.plusSeconds(120));

        AuthResponseDto result = service.refresh(RAW);

        assertThat(old.getRevokedAt()).isEqualTo(START.plusSeconds(120));
        assertThat(result.getRefreshToken()).isNotBlank().isNotEqualTo(RAW).hasSize(43);
        assertThat(result.getAccessToken().split("\\.")).hasSize(3);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(900L);
        assertThat(result.getUser().getUsername()).isEqualTo("kartik");

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(result.getRefreshToken()));
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(START.plusSeconds(120).plus(Duration.ofDays(7)));
        verify(repository, never()).revokeAllActiveByUserId(anyLong(), any());
    }

    // ---------- logout ----------

    @Test
    void logout_revokesActiveToken() {
        RefreshToken active = storedToken(START.plus(Duration.ofDays(1)), null);
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(active));

        service.logout(RAW);

        assertThat(active.getRevokedAt()).isEqualTo(START);
    }

    @Test
    void logout_twiceIsFineAndKeepsFirstRevocationTime() {
        RefreshToken active = storedToken(START.plus(Duration.ofDays(1)), null);
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.of(active));

        service.logout(RAW);
        clock.set(START.plusSeconds(300));
        assertThatCode(() -> service.logout(RAW)).doesNotThrowAnyException();

        assertThat(active.getRevokedAt()).isEqualTo(START);
        verify(repository, never()).revokeAllActiveByUserId(anyLong(), any());
    }

    @Test
    void logout_unknownOrNullTokenReturnsNormally() {
        when(repository.findByTokenHash(RefreshTokenService.hash(RAW))).thenReturn(Optional.empty());

        assertThatCode(() -> service.logout(RAW)).doesNotThrowAnyException();
        assertThatCode(() -> service.logout(null)).doesNotThrowAnyException();
    }

    /** A clock whose time the test can move. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

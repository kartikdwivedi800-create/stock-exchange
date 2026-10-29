package com.nexusexchange.user.service;

import com.nexusexchange.user.config.SecurityProperties;
import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.entity.RefreshToken;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.exception.InvalidRefreshTokenException;
import com.nexusexchange.user.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque refresh tokens: rotation, reuse detection and logout.
 * Only SHA-256(token) is stored; the raw token is returned to the client once and never logged.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;
    private final SecurityProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokens, TokenService tokenService,
                               SecurityProperties properties, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
        this.properties = properties;
        this.clock = clock;
    }

    /** 32 random bytes as Base64URL without padding (43 characters). */
    public static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of the token as lowercase hex (64 characters). */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Creates and stores a new refresh token for the user and returns the RAW value. */
    @Transactional
    public String issue(User user) {
        return issueAt(user, clock.instant());
    }

    /**
     * Rotation: the presented token is revoked and a new access + refresh pair is issued.
     * A revoked token being presented again means it was stolen or replayed: all of the user's
     * active tokens are revoked. The revoke-all must be committed even though we answer 401,
     * hence {@code noRollbackFor}.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponseDto refresh(String rawToken) {
        Instant now = clock.instant();
        if (rawToken == null) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.getRevokedAt() != null) {
            Long userId = stored.getUser().getId();
            int revoked = refreshTokens.revokeAllActiveByUserId(userId, now);
            log.warn("Refresh token reuse detected for user {}; revoked {} active token(s)", userId, revoked);
            throw new InvalidRefreshTokenException();
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        stored.setRevokedAt(now);
        User user = stored.getUser();
        String newRawToken = issueAt(user, now);

        return AuthResponseDto.builder()
                .accessToken(tokenService.createAccessToken(user))
                .expiresIn(tokenService.accessTokenExpiresInSeconds())
                .refreshToken(newRawToken)
                .user(UserProfileDto.from(user))
                .build();
    }

    /** Revokes the token if it exists and is still active. Never fails, never reveals whether it existed. */
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null) {
            return;
        }
        refreshTokens.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(clock.instant()));
    }

    private String issueAt(User user, Instant now) {
        String rawToken = generateRawToken();
        refreshTokens.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(properties.refreshTokenTtl()))
                .build());
        return rawToken;
    }
}

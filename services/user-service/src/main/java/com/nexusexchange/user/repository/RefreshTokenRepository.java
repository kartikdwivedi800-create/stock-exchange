package com.nexusexchange.user.repository;

import com.nexusexchange.user.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Row lock (SELECT ... FOR UPDATE) so two parallel refreshes of one token cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revokes every still-active token of the user; returns how many rows were revoked. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.user.id = :userId and t.revokedAt is null and t.expiresAt > :now")
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}

package com.nexusexchange.user.service;

import com.nexusexchange.user.config.SecurityProperties;
import com.nexusexchange.user.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Issues HS256 access tokens. Validation is done by Spring's resource server, not here. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        // JWT times are whole seconds; truncating keeps exp - iat exactly equal to the TTL
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Duration ttl = properties.jwt().accessTokenTtl();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .id(UUID.randomUUID().toString())
                .build();

        // Without an explicit HS256 header Nimbus assumes RS256 and fails to select a signing key
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenExpiresInSeconds() {
        return properties.jwt().accessTokenTtl().toSeconds();
    }
}

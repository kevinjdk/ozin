package com.ozin.infrastructure.identity.adapter.out.security;

import com.ozin.application.identity.port.out.AccessTokenIssuerPort;
import com.ozin.domain.identity.User;
import com.ozin.infrastructure.config.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    JwtAccessTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().value().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(SecurityProperties.ROLE_CLAIM, user.role().name());
        if (user.profileId() != null) {
            claims.claim(SecurityProperties.PROFILE_CLAIM, user.profileId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }
}

package com.ozin.application.identity.service;

import com.ozin.application.identity.AuthTokens;
import com.ozin.application.identity.SessionPolicy;
import com.ozin.application.identity.port.out.AccessTokenIssuerPort;
import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.domain.identity.RefreshToken;
import com.ozin.domain.identity.User;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Emite el par de tokens de una sesión; lo comparten el inicio de sesión y la renovación. */
public final class SessionTokenService {

    private final AccessTokenIssuerPort accessTokens;
    private final RefreshTokenPort refreshTokens;
    private final SessionPolicy policy;
    private final Clock clock;

    public SessionTokenService(AccessTokenIssuerPort accessTokens,
                               RefreshTokenPort refreshTokens,
                               SessionPolicy policy,
                               Clock clock) {
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.policy = policy;
        this.clock = clock;
    }

    AuthTokens issue(User user, UUID familyId) {
        Instant now = clock.instant();
        String rawRefreshToken = TokenSupport.generate();
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.id(), familyId,
                TokenSupport.hash(rawRefreshToken), now.plus(policy.refreshTokenTtl()), null));
        AccessTokenIssuerPort.IssuedAccessToken access = accessTokens.issue(user);
        return new AuthTokens(access.value(), access.expiresInSeconds(), rawRefreshToken, user.role(), user.profileId());
    }
}

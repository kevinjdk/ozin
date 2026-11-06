package com.ozin.application.identity.service;

import com.ozin.application.identity.AuthTokens;
import com.ozin.application.identity.port.in.RefreshSessionUseCase;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.identity.RefreshToken;
import com.ozin.domain.identity.User;
import com.ozin.domain.shared.InvalidCredentialsException;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Rotación con detección de reutilización: cada token sirve una sola vez. Si llega uno ya usado,
 * alguien pudo haberlo robado, así que se revoca toda la sesión.
 *
 * <p>La excepción se lanza fuera de la transacción: lanzarla dentro desharía justo esa revocación.
 */
public final class RefreshSessionService implements RefreshSessionUseCase {

    private final RefreshTokenPort refreshTokens;
    private final LoadUserPort users;
    private final SessionTokenService sessions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RefreshSessionService(RefreshTokenPort refreshTokens,
                                 LoadUserPort users,
                                 SessionTokenService sessions,
                                 UnitOfWork unitOfWork,
                                 Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.sessions = sessions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        return unitOfWork.inTransaction(() -> rotate(refreshToken))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "INVALID_REFRESH_TOKEN", "La sesión no es válida o ha expirado"));
    }

    private Optional<AuthTokens> rotate(String refreshToken) {
        Instant now = clock.instant();
        Optional<RefreshToken> found = refreshTokens.findByHashForUpdate(TokenSupport.hash(refreshToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken token = found.get();
        if (token.isRevoked()) {
            refreshTokens.revokeFamily(token.familyId(), now);
            return Optional.empty();
        }
        if (token.isExpired(now)) {
            return Optional.empty();
        }
        Optional<User> user = users.findById(token.userId()).filter(User::isEnabled);
        if (user.isEmpty()) {
            refreshTokens.revokeFamily(token.familyId(), now);
            return Optional.empty();
        }
        refreshTokens.save(token.revoke(now));
        return Optional.of(sessions.issue(user.get(), token.familyId()));
    }
}

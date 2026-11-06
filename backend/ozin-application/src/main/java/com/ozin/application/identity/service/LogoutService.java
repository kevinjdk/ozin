package com.ozin.application.identity.service;

import com.ozin.application.identity.port.in.LogoutUseCase;
import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.application.shared.port.out.UnitOfWork;

import java.time.Clock;

public final class LogoutService implements LogoutUseCase {

    private final RefreshTokenPort refreshTokens;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LogoutService(RefreshTokenPort refreshTokens, UnitOfWork unitOfWork, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public void logout(String refreshToken) {
        unitOfWork.inTransaction(() -> {
            refreshTokens.findByHashForUpdate(TokenSupport.hash(refreshToken))
                    .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
            return null;
        });
    }
}

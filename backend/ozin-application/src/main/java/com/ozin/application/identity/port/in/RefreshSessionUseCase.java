package com.ozin.application.identity.port.in;

import com.ozin.application.identity.AuthTokens;

/** Renueva la sesión y rota el token de renovación (ADR-0004). */
public interface RefreshSessionUseCase {

    AuthTokens refresh(String refreshToken);
}

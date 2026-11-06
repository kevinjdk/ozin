package com.ozin.application.identity;

import com.ozin.domain.identity.Role;

import java.util.UUID;

/** Resultado de abrir o renovar una sesión. {@code profileId} es {@code null} para el administrador. */
public record AuthTokens(String accessToken,
                         long accessTokenExpiresInSeconds,
                         String refreshToken,
                         Role role,
                         UUID profileId) {

    @Override
    public String toString() {
        return "AuthTokens[role=" + role + "]";
    }
}

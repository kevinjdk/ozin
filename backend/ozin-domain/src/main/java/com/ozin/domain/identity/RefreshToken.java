package com.ozin.domain.identity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de renovación de una sesión (ADR-0004). Solo se guarda su hash.
 *
 * @param familyId  común a todos los tokens de una misma sesión; permite revocarla por completo
 *                  si se detecta que se reutilizó un token ya usado
 * @param revokedAt momento de revocación, o {@code null} si sigue vigente
 */
public record RefreshToken(UUID id, UserId userId, UUID familyId, String tokenHash, Instant expiresAt, Instant revokedAt) {

    public RefreshToken {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public RefreshToken revoke(Instant now) {
        return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, now);
    }
}

package com.ozin.application.identity.port.out;

import com.ozin.domain.identity.RefreshToken;
import com.ozin.domain.identity.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenPort {

    void save(RefreshToken token);

    /** Bloquea el token hasta el fin de la transacción, para que dos renovaciones simultáneas no lo usen a la vez. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllOf(UserId userId, Instant now);

    void deleteExpiredOf(UserId userId, Instant now);
}

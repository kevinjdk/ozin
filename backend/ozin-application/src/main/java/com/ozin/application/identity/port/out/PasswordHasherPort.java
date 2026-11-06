package com.ozin.application.identity.port.out;

import com.ozin.domain.identity.RawPassword;

public interface PasswordHasherPort {

    String hash(RawPassword password);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Hace el mismo trabajo que una comprobación real cuando el email no existe, para que
     * el tiempo de respuesta no revele qué cuentas están registradas.
     */
    void simulateVerification(String rawPassword);
}

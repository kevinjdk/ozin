package com.ozin.application.identity;

import java.time.Duration;
import java.util.Objects;

/**
 * Parámetros de sesión y de protección del inicio de sesión.
 *
 * @param refreshTokenTtl    vigencia de cada token de renovación
 * @param maxFailedLogins    intentos fallidos permitidos antes de bloquear
 * @param failedLoginWindow  duración del bloqueo y de la ventana en la que se cuentan los fallos
 */
public record SessionPolicy(Duration refreshTokenTtl, int maxFailedLogins, Duration failedLoginWindow) {

    public SessionPolicy {
        Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
        Objects.requireNonNull(failedLoginWindow, "failedLoginWindow");
    }
}

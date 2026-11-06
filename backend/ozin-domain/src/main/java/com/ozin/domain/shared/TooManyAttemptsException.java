package com.ozin.domain.shared;

/** Se lanza cuando se superan los intentos permitidos de inicio de sesión. */
public final class TooManyAttemptsException extends DomainException {

    public TooManyAttemptsException() {
        super("TOO_MANY_ATTEMPTS", "Demasiados intentos fallidos. Vuelve a intentarlo más tarde.");
    }
}

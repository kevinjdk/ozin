package com.ozin.domain.shared;

/**
 * Se lanza cuando no se puede autenticar a quien hace la petición. El mensaje es deliberadamente
 * genérico: no revela si el email existe ni cuál de los datos es incorrecto.
 */
public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException(String code, String message) {
        super(code, message);
    }
}

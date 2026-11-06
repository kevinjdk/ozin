package com.ozin.domain.shared;

/** Se lanza cuando el usuario autenticado no tiene permiso sobre un recurso concreto. */
public final class AccessDeniedException extends DomainException {

    public AccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }
}

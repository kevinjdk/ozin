package com.ozin.domain.shared;

/** Se lanza cuando se intenta construir un objeto de dominio con datos inválidos. */
public final class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}

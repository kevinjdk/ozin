package com.ozin.application.shared;

import com.ozin.domain.shared.DomainValidationException;

/** Página solicitada, empezando en 0. */
public record PageQuery(int page, int size) {

    private static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new DomainValidationException("La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new DomainValidationException("El tamaño de página debe estar entre 1 y %d".formatted(MAX_SIZE));
        }
    }
}

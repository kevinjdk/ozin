package com.ozin.application.booking;

import com.ozin.domain.shared.DomainValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Rango de consulta de una agenda. Se limita para que una consulta no pueda recorrer todo el historial. */
public record AgendaRange(Instant from, Instant to) {

    private static final Duration MAX_LENGTH = Duration.ofDays(31);

    public AgendaRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new DomainValidationException("El inicio del rango debe ser anterior a su fin");
        }
        if (Duration.between(from, to).compareTo(MAX_LENGTH) > 0) {
            throw new DomainValidationException("El rango no puede superar los %d días".formatted(MAX_LENGTH.toDays()));
        }
    }
}

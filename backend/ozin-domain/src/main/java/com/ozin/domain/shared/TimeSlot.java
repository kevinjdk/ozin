package com.ozin.domain.shared;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Intervalo de tiempo semiabierto {@code [start, end)} expresado en UTC (RN-11).
 *
 * <p>Al ser semiabierto, dos intervalos contiguos (uno termina a las 10:00 y el otro empieza a las 10:00)
 * no se solapan, que es lo que se espera de citas consecutivas.
 */
public record TimeSlot(Instant start, Instant end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new DomainValidationException("El inicio del intervalo debe ser anterior a su fin");
        }
    }

    public static TimeSlot of(Instant start, Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new DomainValidationException("La duración debe ser positiva");
        }
        return new TimeSlot(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public boolean overlapsWith(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(TimeSlot other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }
}

package com.ozin.domain.booking;

import com.ozin.domain.shared.DomainValidationException;

import java.time.Duration;
import java.util.Objects;

/**
 * Parámetros configurables de las reglas de reserva.
 *
 * @param minimumAdvance                    RN-04: anticipación mínima para reservar
 * @param maximumAdvance                    RN-05: anticipación máxima para reservar
 * @param slotGranularity                   RN-06: intervalo entre horarios ofrecidos
 * @param maxActiveAppointmentsPerCustomer  RN-08: citas activas máximas por cliente
 */
public record BookingPolicy(Duration minimumAdvance,
                            Duration maximumAdvance,
                            Duration slotGranularity,
                            int maxActiveAppointmentsPerCustomer) {

    public BookingPolicy {
        Objects.requireNonNull(minimumAdvance, "minimumAdvance");
        Objects.requireNonNull(maximumAdvance, "maximumAdvance");
        Objects.requireNonNull(slotGranularity, "slotGranularity");
        if (minimumAdvance.isNegative() || maximumAdvance.compareTo(minimumAdvance) <= 0) {
            throw new DomainValidationException("La anticipación máxima debe ser mayor que la mínima");
        }
        if (slotGranularity.isNegative() || slotGranularity.isZero()
                || Duration.ofDays(1).toSeconds() % slotGranularity.toSeconds() != 0) {
            throw new DomainValidationException("La granularidad debe ser positiva y dividir exactamente un día");
        }
        if (maxActiveAppointmentsPerCustomer < 1) {
            throw new DomainValidationException("El máximo de citas activas debe ser al menos 1");
        }
    }

    /** Valores aprobados en {@code docs/requirements/business-rules.md}. */
    public static BookingPolicy standard() {
        return new BookingPolicy(Duration.ofHours(1), Duration.ofDays(30), Duration.ofMinutes(15), 3);
    }
}

package com.ozin.domain.booking;

import java.time.Instant;

/**
 * Política que decide si una cita puede cancelarse.
 *
 * <p>Principio Abierto/Cerrado: para una regla nueva (por ejemplo, clientes VIP con menos anticipación)
 * se agrega otra implementación sin modificar {@link Appointment} ni las políticas existentes.
 */
@FunctionalInterface
public interface CancellationPolicy {

    /**
     * @throws com.ozin.domain.shared.BusinessRuleViolationException si la cancelación no está permitida
     */
    void ensureCanCancel(Appointment appointment, CancellationActor actor, Instant now);
}

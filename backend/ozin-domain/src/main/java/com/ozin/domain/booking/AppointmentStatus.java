package com.ozin.domain.booking;

/**
 * Estados del ciclo de vida de una cita. Ver {@code docs/architecture/domain-model.md}.
 *
 * <p>No hay estado pendiente: reservar un horario libre ya lo asegura, así que la cita nace confirmada.
 */
public enum AppointmentStatus {
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Una cita activa ocupa el horario del estilista y cuenta para el límite del cliente (RN-01, RN-08). */
    public boolean isActive() {
        return this == CONFIRMED;
    }
}

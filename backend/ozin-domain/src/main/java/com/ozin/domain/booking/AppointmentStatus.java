package com.ozin.domain.booking;

/** Estados del ciclo de vida de una cita. Ver {@code docs/architecture/domain-model.md}. */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Una cita activa ocupa el horario del estilista y cuenta para el límite del cliente (RN-01, RN-08). */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}

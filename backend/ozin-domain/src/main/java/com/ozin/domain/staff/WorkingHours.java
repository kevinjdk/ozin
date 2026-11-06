package com.ozin.domain.staff;

import com.ozin.domain.shared.DomainValidationException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Franja laboral de un estilista para un día de la semana, en hora local de la sede.
 * Un mismo día puede tener varias franjas (por ejemplo, mañana y tarde con pausa para almorzar).
 */
public record WorkingHours(DayOfWeek day, LocalTime start, LocalTime end) {

    public WorkingHours {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new DomainValidationException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    public boolean covers(LocalTime from, LocalTime to) {
        return !from.isBefore(start) && !to.isAfter(end);
    }

    public boolean overlapsWith(WorkingHours other) {
        return day == other.day && start.isBefore(other.end) && other.start.isBefore(end);
    }
}

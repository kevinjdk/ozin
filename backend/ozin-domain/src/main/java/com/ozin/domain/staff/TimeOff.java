package com.ozin.domain.staff;

import com.ozin.domain.shared.TimeSlot;

import java.util.Objects;

/** Ausencia de un estilista (vacaciones, permiso médico, capacitación...). */
public record TimeOff(TimeSlot period, String reason) {

    public TimeOff {
        Objects.requireNonNull(period, "period");
        reason = reason == null ? "" : reason.strip();
    }
}

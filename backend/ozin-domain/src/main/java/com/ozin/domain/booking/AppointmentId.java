package com.ozin.domain.booking;

import java.util.Objects;
import java.util.UUID;

public record AppointmentId(UUID value) {

    public AppointmentId {
        Objects.requireNonNull(value, "value");
    }

    public static AppointmentId newId() {
        return new AppointmentId(UUID.randomUUID());
    }

    public static AppointmentId of(UUID value) {
        return new AppointmentId(value);
    }
}

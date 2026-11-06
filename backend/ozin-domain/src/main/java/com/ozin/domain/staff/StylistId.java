package com.ozin.domain.staff;

import java.util.Objects;
import java.util.UUID;

public record StylistId(UUID value) {

    public StylistId {
        Objects.requireNonNull(value, "value");
    }

    public static StylistId newId() {
        return new StylistId(UUID.randomUUID());
    }

    public static StylistId of(UUID value) {
        return new StylistId(value);
    }
}

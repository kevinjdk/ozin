package com.ozin.domain.catalog;

import java.util.Objects;
import java.util.UUID;

public record SalonServiceId(UUID value) {

    public SalonServiceId {
        Objects.requireNonNull(value, "value");
    }

    public static SalonServiceId newId() {
        return new SalonServiceId(UUID.randomUUID());
    }

    public static SalonServiceId of(UUID value) {
        return new SalonServiceId(value);
    }
}

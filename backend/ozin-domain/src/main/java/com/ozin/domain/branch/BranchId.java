package com.ozin.domain.branch;

import java.util.Objects;
import java.util.UUID;

/** Identificador tipado de una sede: el compilador impide confundirlo con otro tipo de ID. */
public record BranchId(UUID value) {

    public BranchId {
        Objects.requireNonNull(value, "value");
    }

    public static BranchId newId() {
        return new BranchId(UUID.randomUUID());
    }

    public static BranchId of(UUID value) {
        return new BranchId(value);
    }
}

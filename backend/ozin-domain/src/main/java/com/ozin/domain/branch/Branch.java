package com.ozin.domain.branch;

import com.ozin.domain.shared.DomainValidationException;

import java.time.ZoneId;
import java.util.Objects;

/** Sede del salón. Su zona horaria define cómo se interpretan los horarios laborales (RN-11, ADR-0006). */
public record Branch(BranchId id, String name, String address, ZoneId timeZone, boolean active) {

    public Branch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(timeZone, "timeZone");
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("El nombre de la sede es obligatorio");
        }
    }
}

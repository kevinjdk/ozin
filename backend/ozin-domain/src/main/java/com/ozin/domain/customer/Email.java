package com.ozin.domain.customer;

import com.ozin.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Dirección de correo válida y normalizada en minúsculas, para que la unicidad no dependa de mayúsculas. */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null || !FORMAT.matcher(value.strip()).matches()) {
            throw new DomainValidationException("El email no tiene un formato válido");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
    }
}

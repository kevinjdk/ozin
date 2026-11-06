package com.ozin.domain.customer;

import com.ozin.domain.shared.DomainValidationException;

import java.util.regex.Pattern;

/** Número de teléfono en formato internacional E.164, por ejemplo {@code +593991234567}. */
public record PhoneNumber(String value) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    public PhoneNumber {
        if (value == null || !E164.matcher(value).matches()) {
            throw new DomainValidationException("El teléfono debe tener formato internacional, por ejemplo +593991234567");
        }
    }
}

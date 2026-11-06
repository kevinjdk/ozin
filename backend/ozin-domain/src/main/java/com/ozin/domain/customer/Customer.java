package com.ozin.domain.customer;

import com.ozin.domain.shared.DomainValidationException;

import java.util.Objects;

/** Cliente del salón. */
public record Customer(CustomerId id, String fullName, Email email, PhoneNumber phone) {

    /** Código de la regla que impide registrar dos clientes con el mismo email. */
    public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";

    public Customer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(phone, "phone");
        if (fullName == null || fullName.isBlank()) {
            throw new DomainValidationException("El nombre del cliente es obligatorio");
        }
        fullName = fullName.strip();
    }
}

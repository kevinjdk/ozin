package com.ozin.domain.identity;

import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.staff.StylistId;

import java.util.Objects;
import java.util.UUID;

/** Quién ejecuta una operación. Lo construye la capa de entrada a partir del usuario autenticado. */
public record Actor(Role role, UUID profileId) {

    public Actor {
        Objects.requireNonNull(role, "role");
        if ((role == Role.ADMIN) != (profileId == null)) {
            throw new DomainValidationException("Solo el administrador carece de perfil asociado");
        }
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isCustomer(CustomerId customerId) {
        return role == Role.CUSTOMER && profileId.equals(customerId.value());
    }

    public boolean isStylist(StylistId stylistId) {
        return role == Role.STYLIST && profileId.equals(stylistId.value());
    }

    /** Identificador de cliente del actor. Solo tiene sentido si su rol es {@code CUSTOMER}. */
    public CustomerId customerId() {
        if (role != Role.CUSTOMER) {
            throw new DomainValidationException("El actor no es un cliente");
        }
        return CustomerId.of(profileId);
    }
}

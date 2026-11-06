package com.ozin.domain.identity;

import com.ozin.domain.customer.Email;
import com.ozin.domain.shared.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Credenciales de acceso de una persona.
 *
 * <p>El usuario apunta a su perfil de negocio ({@code Customer} o {@code Stylist}) mediante
 * {@code profileId}, de modo que esos perfiles no conocen nada de autenticación.
 */
public final class User {

    private final UserId id;
    private final Email email;
    private final String passwordHash;
    private final Role role;
    private final UUID profileId;
    private boolean enabled;

    public User(UserId id, Email email, String passwordHash, Role role, UUID profileId, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        if ((role == Role.ADMIN) != (profileId == null)) {
            throw new DomainValidationException("Solo el administrador carece de perfil asociado");
        }
        this.profileId = profileId;
        this.enabled = enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    /** {@code null} únicamente para el administrador. */
    public UUID profileId() {
        return profileId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

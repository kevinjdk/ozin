package com.ozin.domain.identity;

import com.ozin.domain.shared.DomainValidationException;

import java.nio.charset.StandardCharsets;

/**
 * Contraseña en texto plano que cumple la política de HU-06: al menos 8 caracteres, una mayúscula y un número.
 *
 * <p>El máximo de 72 bytes es el límite de BCrypt. {@link #toString()} la oculta para
 * que nunca termine en un log.
 */
public record RawPassword(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_BYTES = 72;

    public RawPassword {
        if (value == null || value.length() < MIN_LENGTH
                || value.chars().noneMatch(Character::isUpperCase)
                || value.chars().noneMatch(Character::isDigit)) {
            throw new DomainValidationException(
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new DomainValidationException("La contraseña es demasiado larga");
        }
    }

    @Override
    public String toString() {
        return "RawPassword[***]";
    }
}

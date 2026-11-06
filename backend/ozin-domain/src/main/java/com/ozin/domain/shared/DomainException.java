package com.ozin.domain.shared;

/**
 * Excepción base de todas las violaciones del dominio.
 *
 * <p>Lleva un {@code code} estable que la capa REST traduce a una respuesta HTTP
 * sin necesidad de interpretar el mensaje.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

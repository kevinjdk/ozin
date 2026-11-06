package com.ozin.domain.shared;

/** Se lanza cuando una operación hace referencia a un recurso que no existe. */
public final class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", "%s no encontrado: %s".formatted(resource, id));
    }
}

package com.ozin.application.shared.port.out;

import java.util.function.Supplier;

/**
 * Delimita una transacción sin que la capa de aplicación dependa de Spring (ver ADR-0007).
 * Si la acción lanza una excepción, todos sus cambios se revierten.
 */
public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> action);
}

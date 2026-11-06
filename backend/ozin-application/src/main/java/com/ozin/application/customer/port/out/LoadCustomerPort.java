package com.ozin.application.customer.port.out;

import com.ozin.domain.customer.CustomerId;

public interface LoadCustomerPort {

    boolean existsById(CustomerId id);

    /**
     * Bloquea al cliente hasta el fin de la transacción y devuelve si existe.
     * Serializa sus reservas simultáneas para que no puedan superar el límite de citas activas (RN-08).
     */
    boolean lockForBooking(CustomerId id);
}

package com.ozin.application.staff.service;

import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.DomainValidationException;

import java.util.Set;

/** Validación compartida al crear y actualizar estilistas. */
final class StylistReferences {

    private StylistReferences() {
    }

    static void requireExistingServices(LoadSalonServicePort services, Set<SalonServiceId> serviceIds) {
        if (services.countExisting(serviceIds) != serviceIds.size()) {
            throw new DomainValidationException("Alguno de los servicios indicados no existe");
        }
    }
}

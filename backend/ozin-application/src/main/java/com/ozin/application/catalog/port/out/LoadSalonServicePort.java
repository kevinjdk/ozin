package com.ozin.application.catalog.port.out;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadSalonServicePort {

    Optional<SalonService> findById(SalonServiceId id);

    List<SalonService> findAll(boolean includeInactive);

    /** Devuelve cuántos de los identificadores corresponden a servicios existentes. */
    long countExisting(Collection<SalonServiceId> ids);
}

package com.ozin.application.staff.port.in;

import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.Stylist;

import java.util.List;
import java.util.Optional;

/** HU-02 y HU-11. */
public interface ListStylistsUseCase {

    /** @param serviceId si está presente, solo devuelve los estilistas que realizan ese servicio */
    List<Stylist> list(Optional<SalonServiceId> serviceId, boolean includeInactive);
}

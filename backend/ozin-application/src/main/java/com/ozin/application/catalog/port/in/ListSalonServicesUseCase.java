package com.ozin.application.catalog.port.in;

import com.ozin.domain.catalog.SalonService;

import java.util.List;

/** HU-01 y HU-10. */
public interface ListSalonServicesUseCase {

    List<SalonService> list(boolean includeInactive);
}

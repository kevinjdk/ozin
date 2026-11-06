package com.ozin.application.catalog.service;

import com.ozin.application.catalog.port.in.ListSalonServicesUseCase;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.domain.catalog.SalonService;

import java.util.List;

public final class ListSalonServicesService implements ListSalonServicesUseCase {

    private final LoadSalonServicePort services;

    public ListSalonServicesService(LoadSalonServicePort services) {
        this.services = services;
    }

    @Override
    public List<SalonService> list(boolean includeInactive) {
        return services.findAll(includeInactive);
    }
}

package com.ozin.application.catalog.service;

import com.ozin.application.catalog.port.in.CreateSalonServiceUseCase;
import com.ozin.application.catalog.port.out.SaveSalonServicePort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;

public final class CreateSalonServiceService implements CreateSalonServiceUseCase {

    private final SaveSalonServicePort services;
    private final UnitOfWork unitOfWork;

    public CreateSalonServiceService(SaveSalonServicePort services, UnitOfWork unitOfWork) {
        this.services = services;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public SalonService create(Command command) {
        SalonService service = new SalonService(
                SalonServiceId.newId(), command.name(), command.duration(), command.price(), true);
        return unitOfWork.inTransaction(() -> {
            services.save(service);
            return service;
        });
    }
}

package com.ozin.application.catalog.service;

import com.ozin.application.catalog.port.in.UpdateSalonServiceUseCase;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.catalog.port.out.SaveSalonServicePort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.shared.ResourceNotFoundException;

public final class UpdateSalonServiceService implements UpdateSalonServiceUseCase {

    private final LoadSalonServicePort loadServices;
    private final SaveSalonServicePort saveServices;
    private final UnitOfWork unitOfWork;

    public UpdateSalonServiceService(LoadSalonServicePort loadServices,
                                     SaveSalonServicePort saveServices,
                                     UnitOfWork unitOfWork) {
        this.loadServices = loadServices;
        this.saveServices = saveServices;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public SalonService update(Command command) {
        return unitOfWork.inTransaction(() -> {
            SalonService service = loadServices.findById(command.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio", command.id().value()));
            service.revise(command.name(), command.duration(), command.price(), command.active());
            saveServices.save(service);
            return service;
        });
    }
}

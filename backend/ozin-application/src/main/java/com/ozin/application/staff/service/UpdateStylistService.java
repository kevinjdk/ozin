package com.ozin.application.staff.service;

import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.UpdateStylistUseCase;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;

public final class UpdateStylistService implements UpdateStylistUseCase {

    private final LoadStylistPort loadStylists;
    private final SaveStylistPort saveStylists;
    private final LoadSalonServicePort services;
    private final UnitOfWork unitOfWork;

    public UpdateStylistService(LoadStylistPort loadStylists,
                                SaveStylistPort saveStylists,
                                LoadSalonServicePort services,
                                UnitOfWork unitOfWork) {
        this.loadStylists = loadStylists;
        this.saveStylists = saveStylists;
        this.services = services;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Stylist update(Command command) {
        return unitOfWork.inTransaction(() -> {
            Stylist stylist = loadStylists.findById(command.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Estilista", command.id().value()));
            StylistReferences.requireExistingServices(services, command.serviceIds());
            stylist.revise(command.fullName(), command.active(), command.serviceIds(), command.workingHours());
            saveStylists.save(stylist);
            return stylist;
        });
    }
}

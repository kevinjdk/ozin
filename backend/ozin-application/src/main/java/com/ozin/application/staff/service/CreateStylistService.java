package com.ozin.application.staff.service;

import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.CreateStylistUseCase;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.util.List;

public final class CreateStylistService implements CreateStylistUseCase {

    private final LoadBranchPort branches;
    private final LoadSalonServicePort services;
    private final SaveStylistPort stylists;
    private final UnitOfWork unitOfWork;

    public CreateStylistService(LoadBranchPort branches,
                                LoadSalonServicePort services,
                                SaveStylistPort stylists,
                                UnitOfWork unitOfWork) {
        this.branches = branches;
        this.services = services;
        this.stylists = stylists;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Stylist create(Command command) {
        Stylist stylist = new Stylist(StylistId.newId(), command.branchId(), command.fullName(), true,
                command.serviceIds(), command.workingHours(), List.of());
        return unitOfWork.inTransaction(() -> {
            branches.findById(command.branchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede", command.branchId().value()));
            StylistReferences.requireExistingServices(services, command.serviceIds());
            stylists.save(stylist);
            return stylist;
        });
    }
}

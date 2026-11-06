package com.ozin.application.staff.service;

import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.PasswordHasherPort;
import com.ozin.application.identity.port.out.SaveUserPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.CreateStylistUseCase;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.identity.Role;
import com.ozin.domain.identity.User;
import com.ozin.domain.identity.UserId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.util.List;

public final class CreateStylistService implements CreateStylistUseCase {

    private final LoadBranchPort branches;
    private final LoadSalonServicePort services;
    private final SaveStylistPort stylists;
    private final LoadUserPort loadUsers;
    private final SaveUserPort saveUsers;
    private final PasswordHasherPort hasher;
    private final UnitOfWork unitOfWork;

    public CreateStylistService(LoadBranchPort branches,
                                LoadSalonServicePort services,
                                SaveStylistPort stylists,
                                LoadUserPort loadUsers,
                                SaveUserPort saveUsers,
                                PasswordHasherPort hasher,
                                UnitOfWork unitOfWork) {
        this.branches = branches;
        this.services = services;
        this.stylists = stylists;
        this.loadUsers = loadUsers;
        this.saveUsers = saveUsers;
        this.hasher = hasher;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Stylist create(Command command) {
        Stylist stylist = new Stylist(StylistId.newId(), command.branchId(), command.fullName(), true,
                command.serviceIds(), command.workingHours(), List.of());
        String passwordHash = hasher.hash(command.password());
        return unitOfWork.inTransaction(() -> {
            branches.findById(command.branchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sede", command.branchId().value()));
            StylistReferences.requireExistingServices(services, command.serviceIds());
            if (loadUsers.existsByEmail(command.email())) {
                throw new BusinessRuleViolationException(Customer.EMAIL_ALREADY_REGISTERED, "El email ya está registrado");
            }
            stylists.save(stylist);
            saveUsers.save(new User(UserId.newId(), command.email(), passwordHash, Role.STYLIST,
                    stylist.id().value(), true));
            return stylist;
        });
    }
}

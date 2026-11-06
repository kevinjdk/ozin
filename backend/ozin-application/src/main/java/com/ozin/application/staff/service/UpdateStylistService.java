package com.ozin.application.staff.service;

import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.application.identity.port.out.SaveUserPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.UpdateStylistUseCase;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.identity.User;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;

import java.time.Clock;

public final class UpdateStylistService implements UpdateStylistUseCase {

    private final LoadStylistPort loadStylists;
    private final SaveStylistPort saveStylists;
    private final LoadSalonServicePort services;
    private final LoadUserPort loadUsers;
    private final SaveUserPort saveUsers;
    private final RefreshTokenPort refreshTokens;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdateStylistService(LoadStylistPort loadStylists,
                                SaveStylistPort saveStylists,
                                LoadSalonServicePort services,
                                LoadUserPort loadUsers,
                                SaveUserPort saveUsers,
                                RefreshTokenPort refreshTokens,
                                UnitOfWork unitOfWork,
                                Clock clock) {
        this.loadStylists = loadStylists;
        this.saveStylists = saveStylists;
        this.services = services;
        this.loadUsers = loadUsers;
        this.saveUsers = saveUsers;
        this.refreshTokens = refreshTokens;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Stylist update(Command command) {
        return unitOfWork.inTransaction(() -> {
            Stylist stylist = loadStylists.findById(command.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Estilista", command.id().value()));
            StylistReferences.requireExistingServices(services, command.serviceIds());
            stylist.revise(command.fullName(), command.active(), command.serviceIds(), command.workingHours());
            saveStylists.save(stylist);
            loadUsers.findByStylist(stylist.id()).ifPresent(user -> syncAccess(user, stylist));
            return stylist;
        });
    }

    /** Un estilista dado de baja pierde el acceso: no puede iniciar sesión ni renovar la que tenga abierta. */
    private void syncAccess(User user, Stylist stylist) {
        if (stylist.isActive()) {
            user.enable();
        } else {
            user.disable();
            refreshTokens.revokeAllOf(user.id(), clock.instant());
        }
        saveUsers.save(user);
    }
}

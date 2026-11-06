package com.ozin.application.booking.service;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.application.booking.port.in.GetStylistAgendaUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentViewsPort;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.domain.identity.Actor;
import com.ozin.domain.shared.AccessDeniedException;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.StylistId;

import java.util.List;

public final class GetStylistAgendaService implements GetStylistAgendaUseCase {

    private final LoadStylistPort stylists;
    private final LoadAppointmentViewsPort views;

    public GetStylistAgendaService(LoadStylistPort stylists, LoadAppointmentViewsPort views) {
        this.stylists = stylists;
        this.views = views;
    }

    @Override
    public List<AppointmentView> getAgenda(StylistId stylistId, Actor actor, AgendaRange range) {
        if (!actor.isAdmin() && !actor.isStylist(stylistId)) {
            throw new AccessDeniedException("No puedes ver la agenda de otro estilista");
        }
        if (stylists.findById(stylistId).isEmpty()) {
            throw new ResourceNotFoundException("Estilista", stylistId.value());
        }
        return views.findByStylist(stylistId, range);
    }
}

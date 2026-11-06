package com.ozin.application.booking.port.in;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.domain.identity.Actor;
import com.ozin.domain.staff.StylistId;

import java.util.List;

/** HU-08 y filtro por estilista de HU-13. */
public interface GetStylistAgendaUseCase {

    List<AppointmentView> getAgenda(StylistId stylistId, Actor actor, AgendaRange range);
}

package com.ozin.application.booking.port.in;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.identity.Actor;

/** HU-09. */
public interface ConfirmAppointmentUseCase {

    Appointment confirm(AppointmentId id, Actor actor);
}

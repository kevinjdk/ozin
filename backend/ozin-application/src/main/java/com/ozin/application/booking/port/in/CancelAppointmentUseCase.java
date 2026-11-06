package com.ozin.application.booking.port.in;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.identity.Actor;

/** HU-05. */
public interface CancelAppointmentUseCase {

    Appointment cancel(AppointmentId id, Actor actor);
}

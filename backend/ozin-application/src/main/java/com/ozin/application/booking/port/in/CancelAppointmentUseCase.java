package com.ozin.application.booking.port.in;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.booking.CancellationActor;

/** HU-05. */
public interface CancelAppointmentUseCase {

    Appointment cancel(AppointmentId id, CancellationActor actor);
}

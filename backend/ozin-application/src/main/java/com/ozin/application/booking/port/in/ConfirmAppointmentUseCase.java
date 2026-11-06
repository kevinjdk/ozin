package com.ozin.application.booking.port.in;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;

/** HU-09. */
public interface ConfirmAppointmentUseCase {

    Appointment confirm(AppointmentId id);
}

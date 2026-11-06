package com.ozin.application.booking.port.in;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.staff.StylistId;

import java.time.Instant;

/** HU-03. */
public interface BookAppointmentUseCase {

    Appointment book(Command command);

    record Command(CustomerId customerId, StylistId stylistId, SalonServiceId serviceId, Instant start) {
    }
}

package com.ozin.application.booking.port.out;

import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.StylistId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadAppointmentPort {

    Optional<Appointment> findById(AppointmentId id);

    List<Appointment> findActiveByStylistOverlapping(StylistId stylistId, TimeSlot period);

    /** RN-08: cuenta las citas activas del cliente que aún no han empezado. */
    long countUpcomingActiveByCustomer(CustomerId customerId, Instant now);
}

package com.ozin.domain.booking;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.staff.Stylist;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Solicitud de reserva.
 *
 * @param branchZone zona horaria de la sede del estilista; los horarios laborales se interpretan en ella (RN-11)
 */
public record BookingRequest(CustomerId customerId,
                             Stylist stylist,
                             SalonService service,
                             Instant start,
                             ZoneId branchZone) {

    public BookingRequest {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(stylist, "stylist");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(branchZone, "branchZone");
    }
}

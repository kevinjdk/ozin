package com.ozin.application.booking.port.in;

import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.StylistId;

import java.time.LocalDate;
import java.util.List;

/** HU-02. */
public interface GetAvailableSlotsUseCase {

    /** @param date fecha local de la sede del estilista */
    List<TimeSlot> getAvailableSlots(StylistId stylistId, SalonServiceId serviceId, LocalDate date);
}

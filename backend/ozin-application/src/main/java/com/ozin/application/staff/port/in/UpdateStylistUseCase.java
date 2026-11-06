package com.ozin.application.staff.port.in;

import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.WorkingHours;

import java.util.List;
import java.util.Set;

/** HU-11 y HU-12. */
public interface UpdateStylistUseCase {

    Stylist update(Command command);

    record Command(StylistId id, String fullName, boolean active,
                   Set<SalonServiceId> serviceIds, List<WorkingHours> workingHours) {
    }
}

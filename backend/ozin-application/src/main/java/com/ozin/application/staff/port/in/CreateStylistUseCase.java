package com.ozin.application.staff.port.in;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.WorkingHours;

import java.util.Set;
import java.util.List;

/** HU-11 y HU-12. */
public interface CreateStylistUseCase {

    Stylist create(Command command);

    record Command(BranchId branchId, String fullName, Set<SalonServiceId> serviceIds, List<WorkingHours> workingHours) {
    }
}

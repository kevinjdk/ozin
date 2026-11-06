package com.ozin.application.staff.port.in;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.WorkingHours;

import java.util.List;
import java.util.Set;

/** HU-11 y HU-12: crea el estilista junto con sus credenciales de acceso. */
public interface CreateStylistUseCase {

    Stylist create(Command command);

    record Command(BranchId branchId,
                   String fullName,
                   Email email,
                   RawPassword password,
                   Set<SalonServiceId> serviceIds,
                   List<WorkingHours> workingHours) {
    }
}

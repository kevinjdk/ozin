package com.ozin.application.booking.port.in;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.domain.branch.BranchId;

import java.util.List;

/** HU-13. */
public interface GetBranchAgendaUseCase {

    List<AppointmentView> getAgenda(BranchId branchId, AgendaRange range);
}

package com.ozin.application.booking.service;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.application.booking.port.in.GetBranchAgendaUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentViewsPort;
import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.shared.ResourceNotFoundException;

import java.util.List;

public final class GetBranchAgendaService implements GetBranchAgendaUseCase {

    private final LoadBranchPort branches;
    private final LoadAppointmentViewsPort views;

    public GetBranchAgendaService(LoadBranchPort branches, LoadAppointmentViewsPort views) {
        this.branches = branches;
        this.views = views;
    }

    @Override
    public List<AppointmentView> getAgenda(BranchId branchId, AgendaRange range) {
        if (branches.findById(branchId).isEmpty()) {
            throw new ResourceNotFoundException("Sede", branchId.value());
        }
        return views.findByBranch(branchId, range);
    }
}

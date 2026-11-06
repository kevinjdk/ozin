package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.GetAvailableSlotsUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.domain.booking.AppointmentBooker;
import com.ozin.domain.branch.Branch;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GetAvailableSlotsService implements GetAvailableSlotsUseCase {

    private final LoadStylistPort stylists;
    private final LoadSalonServicePort services;
    private final LoadBranchPort branches;
    private final LoadAppointmentPort appointments;
    private final AppointmentBooker booker;
    private final Clock clock;

    public GetAvailableSlotsService(LoadStylistPort stylists,
                                    LoadSalonServicePort services,
                                    LoadBranchPort branches,
                                    LoadAppointmentPort appointments,
                                    AppointmentBooker booker,
                                    Clock clock) {
        this.stylists = stylists;
        this.services = services;
        this.branches = branches;
        this.appointments = appointments;
        this.booker = booker;
        this.clock = clock;
    }

    @Override
    public List<TimeSlot> getAvailableSlots(StylistId stylistId, SalonServiceId serviceId, LocalDate date) {
        Stylist stylist = stylists.findById(stylistId)
                .orElseThrow(() -> new ResourceNotFoundException("Estilista", stylistId.value()));
        SalonService service = services.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", serviceId.value()));
        Branch branch = branches.findById(stylist.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", stylist.branchId().value()));

        TimeSlot wholeDay = new TimeSlot(
                date.atStartOfDay(branch.timeZone()).toInstant(),
                date.plusDays(1).atStartOfDay(branch.timeZone()).toInstant());

        return booker.availableSlots(stylist, service, date, branch.timeZone(),
                appointments.findActiveByStylistOverlapping(stylistId, wholeDay), clock.instant());
    }
}

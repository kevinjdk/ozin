package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.BookAppointmentUseCase;
import com.ozin.application.booking.port.out.AppointmentNotificationPort;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.customer.port.out.LoadCustomerPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentBooker;
import com.ozin.domain.booking.BookingRequest;
import com.ozin.domain.branch.Branch;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Orquesta la reserva: carga los datos, delega las reglas en {@link AppointmentBooker}, guarda
 * y notifica. No contiene reglas de negocio propias.
 */
public final class BookAppointmentService implements BookAppointmentUseCase {

    private final LoadCustomerPort customers;
    private final LoadStylistPort stylists;
    private final LoadSalonServicePort services;
    private final LoadBranchPort branches;
    private final LoadAppointmentPort loadAppointments;
    private final SaveAppointmentPort saveAppointments;
    private final AppointmentNotificationPort notifications;
    private final AppointmentBooker booker;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public BookAppointmentService(LoadCustomerPort customers,
                                  LoadStylistPort stylists,
                                  LoadSalonServicePort services,
                                  LoadBranchPort branches,
                                  LoadAppointmentPort loadAppointments,
                                  SaveAppointmentPort saveAppointments,
                                  AppointmentNotificationPort notifications,
                                  AppointmentBooker booker,
                                  UnitOfWork unitOfWork,
                                  Clock clock) {
        this.customers = customers;
        this.stylists = stylists;
        this.services = services;
        this.branches = branches;
        this.loadAppointments = loadAppointments;
        this.saveAppointments = saveAppointments;
        this.notifications = notifications;
        this.booker = booker;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Appointment book(Command command) {
        Appointment appointment = unitOfWork.inTransaction(() -> bookInTransaction(command));
        notifications.appointmentBooked(appointment);
        return appointment;
    }

    private Appointment bookInTransaction(Command command) {
        if (!customers.lockForBooking(command.customerId())) {
            throw new ResourceNotFoundException("Cliente", command.customerId().value());
        }
        Stylist stylist = stylists.findById(command.stylistId())
                .orElseThrow(() -> new ResourceNotFoundException("Estilista", command.stylistId().value()));
        SalonService service = services.findById(command.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", command.serviceId().value()));
        Branch branch = branches.findById(stylist.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", stylist.branchId().value()));

        Instant now = clock.instant();
        TimeSlot requested = TimeSlot.of(command.start(), service.duration());
        List<Appointment> stylistAppointments =
                loadAppointments.findActiveByStylistOverlapping(stylist.id(), requested);
        long customerActive = loadAppointments.countUpcomingActiveByCustomer(command.customerId(), now);

        Appointment appointment = booker.book(
                new BookingRequest(command.customerId(), stylist, service, command.start(), branch.timeZone()),
                stylistAppointments, customerActive, now);
        saveAppointments.save(appointment);
        return appointment;
    }
}

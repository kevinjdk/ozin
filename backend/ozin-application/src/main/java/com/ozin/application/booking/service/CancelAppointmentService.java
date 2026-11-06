package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.CancelAppointmentUseCase;
import com.ozin.application.booking.port.out.AppointmentNotificationPort;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.booking.CancellationActor;
import com.ozin.domain.booking.CancellationPolicy;
import com.ozin.domain.shared.ResourceNotFoundException;

import java.time.Clock;

public final class CancelAppointmentService implements CancelAppointmentUseCase {

    private final LoadAppointmentPort loadAppointments;
    private final SaveAppointmentPort saveAppointments;
    private final AppointmentNotificationPort notifications;
    private final CancellationPolicy policy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CancelAppointmentService(LoadAppointmentPort loadAppointments,
                                    SaveAppointmentPort saveAppointments,
                                    AppointmentNotificationPort notifications,
                                    CancellationPolicy policy,
                                    UnitOfWork unitOfWork,
                                    Clock clock) {
        this.loadAppointments = loadAppointments;
        this.saveAppointments = saveAppointments;
        this.notifications = notifications;
        this.policy = policy;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Appointment cancel(AppointmentId id, CancellationActor actor) {
        Appointment cancelled = unitOfWork.inTransaction(() -> {
            Appointment appointment = loadAppointments.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", id.value()));
            appointment.cancel(actor, clock.instant(), policy);
            saveAppointments.save(appointment);
            return appointment;
        });
        notifications.appointmentCancelled(cancelled);
        return cancelled;
    }
}

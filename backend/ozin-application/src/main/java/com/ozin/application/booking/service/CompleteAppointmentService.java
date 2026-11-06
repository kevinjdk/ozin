package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.CompleteAppointmentUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.identity.Actor;
import com.ozin.domain.shared.ResourceNotFoundException;

import java.time.Clock;

public final class CompleteAppointmentService implements CompleteAppointmentUseCase {

    private final LoadAppointmentPort loadAppointments;
    private final SaveAppointmentPort saveAppointments;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CompleteAppointmentService(LoadAppointmentPort loadAppointments,
                                      SaveAppointmentPort saveAppointments,
                                      UnitOfWork unitOfWork,
                                      Clock clock) {
        this.loadAppointments = loadAppointments;
        this.saveAppointments = saveAppointments;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Appointment complete(AppointmentId id, Actor actor) {
        return unitOfWork.inTransaction(() -> {
            Appointment appointment = loadAppointments.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", id.value()));
            appointment.ensureManageableBy(actor);
            appointment.complete(clock.instant());
            saveAppointments.save(appointment);
            return appointment;
        });
    }
}

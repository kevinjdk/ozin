package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.ConfirmAppointmentUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.identity.Actor;
import com.ozin.domain.shared.ResourceNotFoundException;

public final class ConfirmAppointmentService implements ConfirmAppointmentUseCase {

    private final LoadAppointmentPort loadAppointments;
    private final SaveAppointmentPort saveAppointments;
    private final UnitOfWork unitOfWork;

    public ConfirmAppointmentService(LoadAppointmentPort loadAppointments,
                                     SaveAppointmentPort saveAppointments,
                                     UnitOfWork unitOfWork) {
        this.loadAppointments = loadAppointments;
        this.saveAppointments = saveAppointments;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Appointment confirm(AppointmentId id, Actor actor) {
        return unitOfWork.inTransaction(() -> {
            Appointment appointment = loadAppointments.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", id.value()));
            appointment.ensureManageableBy(actor);
            appointment.confirm();
            saveAppointments.save(appointment);
            return appointment;
        });
    }
}

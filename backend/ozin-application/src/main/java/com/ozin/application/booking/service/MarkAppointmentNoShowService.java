package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.MarkAppointmentNoShowUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.shared.ResourceNotFoundException;

import java.time.Clock;

public final class MarkAppointmentNoShowService implements MarkAppointmentNoShowUseCase {

    private final LoadAppointmentPort loadAppointments;
    private final SaveAppointmentPort saveAppointments;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public MarkAppointmentNoShowService(LoadAppointmentPort loadAppointments,
                                        SaveAppointmentPort saveAppointments,
                                        UnitOfWork unitOfWork,
                                        Clock clock) {
        this.loadAppointments = loadAppointments;
        this.saveAppointments = saveAppointments;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Appointment markNoShow(AppointmentId id) {
        return unitOfWork.inTransaction(() -> {
            Appointment appointment = loadAppointments.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", id.value()));
            appointment.markNoShow(clock.instant());
            saveAppointments.save(appointment);
            return appointment;
        });
    }
}

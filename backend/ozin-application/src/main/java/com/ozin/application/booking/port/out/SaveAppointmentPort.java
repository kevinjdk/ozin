package com.ozin.application.booking.port.out;

import com.ozin.domain.booking.Appointment;

public interface SaveAppointmentPort {

    /**
     * @throws com.ozin.domain.shared.BusinessRuleViolationException con código RN-01 si otra reserva
     *         simultánea ocupó el horario entre la validación y el guardado
     */
    void save(Appointment appointment);
}

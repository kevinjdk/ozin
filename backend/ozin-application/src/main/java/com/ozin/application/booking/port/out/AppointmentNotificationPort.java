package com.ozin.application.booking.port.out;

import com.ozin.domain.booking.Appointment;

/**
 * Avisos al cliente. Se invoca después de confirmar la transacción, y una implementación
 * no debe propagar errores de envío: un fallo del correo no puede deshacer una reserva ya guardada.
 */
public interface AppointmentNotificationPort {

    void appointmentBooked(Appointment appointment);

    void appointmentCancelled(Appointment appointment);
}

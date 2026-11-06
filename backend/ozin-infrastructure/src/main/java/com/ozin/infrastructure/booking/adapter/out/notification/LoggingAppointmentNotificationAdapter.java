package com.ozin.infrastructure.booking.adapter.out.notification;

import com.ozin.application.booking.port.out.AppointmentNotificationPort;
import com.ozin.domain.booking.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación provisional: registra el aviso en el log y no envía correos.
 * El adaptador SMTP la reemplazará sin tocar los casos de uso.
 */
@Component
class LoggingAppointmentNotificationAdapter implements AppointmentNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAppointmentNotificationAdapter.class);

    @Override
    public void appointmentBooked(Appointment appointment) {
        log.info("Notificación pendiente: cita reservada id={}", appointment.id().value());
    }

    @Override
    public void appointmentCancelled(Appointment appointment) {
        log.info("Notificación pendiente: cita cancelada id={}", appointment.id().value());
    }
}

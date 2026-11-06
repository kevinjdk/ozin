package com.ozin.application.booking;

import com.ozin.domain.booking.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de lectura de una cita para listados y agendas: ya incluye los nombres del cliente,
 * del estilista y del servicio, sin cargar cada agregado por separado.
 */
public record AppointmentView(UUID id,
                              UUID customerId,
                              String customerName,
                              UUID stylistId,
                              String stylistName,
                              UUID serviceId,
                              String serviceName,
                              UUID branchId,
                              Instant start,
                              Instant end,
                              BigDecimal price,
                              String currency,
                              AppointmentStatus status) {
}

package com.ozin.infrastructure.booking.adapter.in.rest;

import com.ozin.application.booking.port.in.BookAppointmentUseCase;
import com.ozin.application.booking.port.in.CancelAppointmentUseCase;
import com.ozin.application.booking.port.in.CompleteAppointmentUseCase;
import com.ozin.application.booking.port.in.ConfirmAppointmentUseCase;
import com.ozin.application.booking.port.in.MarkAppointmentNoShowUseCase;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.booking.AppointmentStatus;
import com.ozin.domain.booking.CancellationActor;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.staff.StylistId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Reserva y ciclo de vida de una cita.
 *
 * <p>Provisional hasta la Fase 4: {@code customerId} y {@code actor} llegan en la petición.
 * Con JWT se obtendrán del usuario autenticado, porque no se puede confiar en lo que envíe el cliente.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Citas")
class AppointmentController {

    private final BookAppointmentUseCase bookAppointment;
    private final ConfirmAppointmentUseCase confirmAppointment;
    private final CancelAppointmentUseCase cancelAppointment;
    private final CompleteAppointmentUseCase completeAppointment;
    private final MarkAppointmentNoShowUseCase markNoShow;

    AppointmentController(BookAppointmentUseCase bookAppointment,
                          ConfirmAppointmentUseCase confirmAppointment,
                          CancelAppointmentUseCase cancelAppointment,
                          CompleteAppointmentUseCase completeAppointment,
                          MarkAppointmentNoShowUseCase markNoShow) {
        this.bookAppointment = bookAppointment;
        this.confirmAppointment = confirmAppointment;
        this.cancelAppointment = cancelAppointment;
        this.completeAppointment = completeAppointment;
        this.markNoShow = markNoShow;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva una cita")
    AppointmentResponse book(@Valid @RequestBody BookAppointmentRequest request) {
        return AppointmentResponse.from(bookAppointment.book(new BookAppointmentUseCase.Command(
                CustomerId.of(request.customerId()), StylistId.of(request.stylistId()),
                SalonServiceId.of(request.serviceId()), request.start())));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirma una cita pendiente")
    AppointmentResponse confirm(@PathVariable UUID id) {
        return AppointmentResponse.from(confirmAppointment.confirm(AppointmentId.of(id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela una cita respetando la política de cancelación")
    AppointmentResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelAppointmentRequest request) {
        return AppointmentResponse.from(cancelAppointment.cancel(AppointmentId.of(id), request.actor()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marca una cita confirmada como completada")
    AppointmentResponse complete(@PathVariable UUID id) {
        return AppointmentResponse.from(completeAppointment.complete(AppointmentId.of(id)));
    }

    @PostMapping("/{id}/no-show")
    @Operation(summary = "Marca que el cliente no asistió a una cita confirmada")
    AppointmentResponse noShow(@PathVariable UUID id) {
        return AppointmentResponse.from(markNoShow.markNoShow(AppointmentId.of(id)));
    }

    record BookAppointmentRequest(@NotNull UUID customerId,
                                  @NotNull UUID stylistId,
                                  @NotNull UUID serviceId,
                                  @NotNull Instant start) {
    }

    record CancelAppointmentRequest(@NotNull CancellationActor actor) {
    }

    record AppointmentResponse(UUID id, UUID customerId, UUID stylistId, UUID serviceId, UUID branchId,
                               Instant start, Instant end, BigDecimal price, String currency,
                               AppointmentStatus status) {

        static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(appointment.id().value(), appointment.customerId().value(),
                    appointment.stylistId().value(), appointment.serviceId().value(), appointment.branchId().value(),
                    appointment.slot().start(), appointment.slot().end(), appointment.price().amount(),
                    appointment.price().currency().getCurrencyCode(), appointment.status());
        }
    }
}

package com.ozin.infrastructure.booking.adapter.in.rest;

import com.ozin.application.booking.port.in.BookAppointmentUseCase;
import com.ozin.application.booking.port.in.CancelAppointmentUseCase;
import com.ozin.application.booking.port.in.CompleteAppointmentUseCase;
import com.ozin.application.booking.port.in.ConfirmAppointmentUseCase;
import com.ozin.application.booking.port.in.MarkAppointmentNoShowUseCase;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.booking.AppointmentStatus;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.StylistId;
import com.ozin.infrastructure.shared.adapter.in.rest.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * <p>El cliente y el actor salen siempre del token validado, nunca del cuerpo de la petición.
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
    AppointmentResponse book(@Valid @RequestBody BookAppointmentRequest request, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AppointmentResponse.from(bookAppointment.book(new BookAppointmentUseCase.Command(
                CurrentActor.from(jwt).customerId(), StylistId.of(request.stylistId()),
                SalonServiceId.of(request.serviceId()), request.start())));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirma una cita pendiente")
    AppointmentResponse confirm(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AppointmentResponse.from(confirmAppointment.confirm(AppointmentId.of(id), CurrentActor.from(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela una cita respetando la política de cancelación")
    AppointmentResponse cancel(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AppointmentResponse.from(cancelAppointment.cancel(AppointmentId.of(id), CurrentActor.from(jwt)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marca una cita confirmada como completada")
    AppointmentResponse complete(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AppointmentResponse.from(completeAppointment.complete(AppointmentId.of(id), CurrentActor.from(jwt)));
    }

    @PostMapping("/{id}/no-show")
    @Operation(summary = "Marca que el cliente no asistió a una cita confirmada")
    AppointmentResponse noShow(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return AppointmentResponse.from(markNoShow.markNoShow(AppointmentId.of(id), CurrentActor.from(jwt)));
    }

    record BookAppointmentRequest(@NotNull UUID stylistId,
                                  @NotNull UUID serviceId,
                                  @NotNull Instant start) {
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

package com.ozin.infrastructure.booking.adapter.in.rest;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.application.booking.port.in.GetAvailableSlotsUseCase;
import com.ozin.application.booking.port.in.GetBranchAgendaUseCase;
import com.ozin.application.booking.port.in.GetStylistAgendaUseCase;
import com.ozin.application.booking.port.in.ListCustomerAppointmentsUseCase;
import com.ozin.application.shared.PageQuery;
import com.ozin.application.shared.PageResult;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.StylistId;
import com.ozin.infrastructure.shared.adapter.in.rest.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consultas de disponibilidad y agendas. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Disponibilidad y agendas")
class AgendaController {

    private final GetAvailableSlotsUseCase getAvailableSlots;
    private final GetStylistAgendaUseCase getStylistAgenda;
    private final GetBranchAgendaUseCase getBranchAgenda;
    private final ListCustomerAppointmentsUseCase listCustomerAppointments;

    AgendaController(GetAvailableSlotsUseCase getAvailableSlots,
                     GetStylistAgendaUseCase getStylistAgenda,
                     GetBranchAgendaUseCase getBranchAgenda,
                     ListCustomerAppointmentsUseCase listCustomerAppointments) {
        this.getAvailableSlots = getAvailableSlots;
        this.getStylistAgenda = getStylistAgenda;
        this.getBranchAgenda = getBranchAgenda;
        this.listCustomerAppointments = listCustomerAppointments;
    }

    @GetMapping("/stylists/{stylistId}/availability")
    @Operation(summary = "Horarios libres de un estilista para un servicio en una fecha local de la sede")
    List<SlotResponse> availability(@PathVariable UUID stylistId,
                                    @RequestParam UUID serviceId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return getAvailableSlots.getAvailableSlots(StylistId.of(stylistId), SalonServiceId.of(serviceId), date)
                .stream()
                .map(slot -> new SlotResponse(slot.start(), slot.end()))
                .toList();
    }

    @GetMapping("/stylists/{stylistId}/agenda")
    @Operation(summary = "Agenda de un estilista en un rango de hasta 31 días")
    List<AppointmentView> stylistAgenda(@PathVariable UUID stylistId,
                                        @RequestParam Instant from,
                                        @RequestParam Instant to,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return getStylistAgenda.getAgenda(StylistId.of(stylistId), CurrentActor.from(jwt), new AgendaRange(from, to));
    }

    @GetMapping("/branches/{branchId}/agenda")
    @Operation(summary = "Agenda general de una sede en un rango de hasta 31 días")
    List<AppointmentView> branchAgenda(@PathVariable UUID branchId,
                                       @RequestParam Instant from,
                                       @RequestParam Instant to) {
        return getBranchAgenda.getAgenda(BranchId.of(branchId), new AgendaRange(from, to));
    }

    @GetMapping("/customers/me/appointments")
    @Operation(summary = "Citas del cliente autenticado, de la más reciente a la más antigua")
    PageResult<AppointmentView> myAppointments(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return listCustomerAppointments.list(CurrentActor.from(jwt).customerId(), new PageQuery(page, size));
    }

    record SlotResponse(Instant start, Instant end) {
    }
}

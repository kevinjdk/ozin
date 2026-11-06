package com.ozin.infrastructure.staff.adapter.in.rest;

import com.ozin.application.staff.port.in.AddTimeOffUseCase;
import com.ozin.application.staff.port.in.CreateStylistUseCase;
import com.ozin.application.staff.port.in.ListStylistsUseCase;
import com.ozin.application.staff.port.in.UpdateStylistUseCase;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;
import com.ozin.domain.staff.WorkingHours;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stylists")
@Tag(name = "Estilistas")
class StylistController {

    private final ListStylistsUseCase listStylists;
    private final CreateStylistUseCase createStylist;
    private final UpdateStylistUseCase updateStylist;
    private final AddTimeOffUseCase addTimeOff;

    StylistController(ListStylistsUseCase listStylists,
                      CreateStylistUseCase createStylist,
                      UpdateStylistUseCase updateStylist,
                      AddTimeOffUseCase addTimeOff) {
        this.listStylists = listStylists;
        this.createStylist = createStylist;
        this.updateStylist = updateStylist;
        this.addTimeOff = addTimeOff;
    }

    @GetMapping
    @Operation(summary = "Lista estilistas activos, opcionalmente solo los que realizan un servicio")
    List<PublicStylistResponse> list(@RequestParam Optional<UUID> serviceId) {
        return listStylists.list(serviceId.map(SalonServiceId::of), false).stream()
                .map(PublicStylistResponse::from)
                .toList();
    }

    @GetMapping("/manage")
    @Operation(summary = "Lista todos los estilistas con sus horarios y ausencias")
    List<StylistResponse> listForManagement() {
        return listStylists.list(Optional.empty(), true).stream().map(StylistResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un estilista con sus servicios y horario")
    StylistResponse create(@Valid @RequestBody CreateStylistRequest request) {
        return StylistResponse.from(createStylist.create(new CreateStylistUseCase.Command(
                BranchId.of(request.branchId()), request.fullName(), new Email(request.email()),
                new RawPassword(request.password()), toServiceIds(request.serviceIds()), toWorkingHours(request.workingHours()))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un estilista; active=false es la baja lógica")
    StylistResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStylistRequest request) {
        return StylistResponse.from(updateStylist.update(new UpdateStylistUseCase.Command(
                StylistId.of(id), request.fullName(), request.active(),
                toServiceIds(request.serviceIds()), toWorkingHours(request.workingHours()))));
    }

    @PostMapping("/{id}/time-offs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una ausencia del estilista")
    StylistResponse addTimeOff(@PathVariable UUID id, @Valid @RequestBody AddTimeOffRequest request) {
        return StylistResponse.from(addTimeOff.addTimeOff(StylistId.of(id),
                new TimeOff(new TimeSlot(request.start(), request.end()), request.reason())));
    }

    private static Set<SalonServiceId> toServiceIds(Set<UUID> ids) {
        return ids.stream().map(SalonServiceId::of).collect(Collectors.toSet());
    }

    private static List<WorkingHours> toWorkingHours(List<WorkingHoursDto> dtos) {
        return dtos.stream().map(dto -> new WorkingHours(dto.day(), dto.start(), dto.end())).toList();
    }

    record WorkingHoursDto(@NotNull DayOfWeek day, @NotNull LocalTime start, @NotNull LocalTime end) {
    }

    record CreateStylistRequest(
            @NotNull UUID branchId,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 254) String email,
            @NotBlank String password,
            @NotNull Set<@NotNull UUID> serviceIds,
            @NotNull List<@NotNull @Valid WorkingHoursDto> workingHours) {

        @Override
        public String toString() {
            return "CreateStylistRequest[***]";
        }
    }

    record UpdateStylistRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotNull Boolean active,
            @NotNull Set<@NotNull UUID> serviceIds,
            @NotNull List<@NotNull @Valid WorkingHoursDto> workingHours) {
    }

    record AddTimeOffRequest(@NotNull Instant start, @NotNull Instant end, @Size(max = 255) String reason) {
    }

    record TimeOffDto(Instant start, Instant end, String reason) {
    }

    /** Lo que puede ver cualquier visitante: sin ausencias, que pueden incluir motivos personales. */
    record PublicStylistResponse(UUID id, UUID branchId, String fullName, Set<UUID> serviceIds) {

        static PublicStylistResponse from(Stylist stylist) {
            return new PublicStylistResponse(stylist.id().value(), stylist.branchId().value(), stylist.fullName(),
                    stylist.services().stream().map(SalonServiceId::value).collect(Collectors.toSet()));
        }
    }

    record StylistResponse(UUID id, UUID branchId, String fullName, boolean active, Set<UUID> serviceIds,
                           List<WorkingHoursDto> workingHours, List<TimeOffDto> timeOffs) {

        static StylistResponse from(Stylist stylist) {
            return new StylistResponse(stylist.id().value(), stylist.branchId().value(), stylist.fullName(),
                    stylist.isActive(),
                    stylist.services().stream().map(SalonServiceId::value).collect(Collectors.toSet()),
                    stylist.workingHours().stream()
                            .sorted(Comparator.comparing(WorkingHours::day).thenComparing(WorkingHours::start))
                            .map(wh -> new WorkingHoursDto(wh.day(), wh.start(), wh.end()))
                            .toList(),
                    stylist.timeOffs().stream()
                            .sorted(Comparator.comparing(off -> off.period().start()))
                            .map(off -> new TimeOffDto(off.period().start(), off.period().end(), off.reason()))
                            .toList());
        }
    }
}

package com.ozin.infrastructure.catalog.adapter.in.rest;

import com.ozin.application.catalog.port.in.CreateSalonServiceUseCase;
import com.ozin.application.catalog.port.in.ListSalonServicesUseCase;
import com.ozin.application.catalog.port.in.UpdateSalonServiceUseCase;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Catálogo de servicios")
class SalonServiceController {

    private final ListSalonServicesUseCase listServices;
    private final CreateSalonServiceUseCase createService;
    private final UpdateSalonServiceUseCase updateService;

    SalonServiceController(ListSalonServicesUseCase listServices,
                           CreateSalonServiceUseCase createService,
                           UpdateSalonServiceUseCase updateService) {
        this.listServices = listServices;
        this.createService = createService;
        this.updateService = updateService;
    }

    @GetMapping
    @Operation(summary = "Lista los servicios activos del catálogo")
    List<SalonServiceResponse> list() {
        return listServices.list(false).stream().map(SalonServiceResponse::from).toList();
    }

    @GetMapping("/manage")
    @Operation(summary = "Lista todos los servicios, incluidos los inactivos")
    List<SalonServiceResponse> listForManagement() {
        return listServices.list(true).stream().map(SalonServiceResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un servicio")
    SalonServiceResponse create(@Valid @RequestBody CreateSalonServiceRequest request) {
        return SalonServiceResponse.from(createService.create(new CreateSalonServiceUseCase.Command(
                request.name(), Duration.ofMinutes(request.durationMinutes()), Money.usd(request.price()))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un servicio; las citas ya reservadas conservan su precio")
    SalonServiceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSalonServiceRequest request) {
        return SalonServiceResponse.from(updateService.update(new UpdateSalonServiceUseCase.Command(
                SalonServiceId.of(id), request.name(), Duration.ofMinutes(request.durationMinutes()),
                Money.usd(request.price()), request.active())));
    }

    record CreateSalonServiceRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(1) @Max(480) Integer durationMinutes,
            @NotNull @PositiveOrZero BigDecimal price) {
    }

    record UpdateSalonServiceRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(1) @Max(480) Integer durationMinutes,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotNull Boolean active) {
    }

    record SalonServiceResponse(UUID id, String name, long durationMinutes, BigDecimal price, String currency,
                                boolean active) {

        static SalonServiceResponse from(SalonService service) {
            return new SalonServiceResponse(service.id().value(), service.name(), service.duration().toMinutes(),
                    service.price().amount(), service.price().currency().getCurrencyCode(), service.isActive());
        }
    }
}

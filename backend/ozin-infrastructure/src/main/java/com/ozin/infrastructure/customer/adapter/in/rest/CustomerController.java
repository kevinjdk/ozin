package com.ozin.infrastructure.customer.adapter.in.rest;

import com.ozin.application.customer.port.in.RegisterCustomerUseCase;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.Email;
import com.ozin.domain.customer.PhoneNumber;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Clientes")
class CustomerController {

    private final RegisterCustomerUseCase registerCustomer;

    CustomerController(RegisterCustomerUseCase registerCustomer) {
        this.registerCustomer = registerCustomer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente")
    CustomerResponse register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = registerCustomer.register(new RegisterCustomerUseCase.Command(
                request.fullName(), new Email(request.email()), new PhoneNumber(request.phone())));
        return new CustomerResponse(customer.id().value(), customer.fullName(),
                customer.email().value(), customer.phone().value());
    }

    record RegisterCustomerRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 254) String email,
            @NotBlank String phone) {
    }

    record CustomerResponse(UUID id, String fullName, String email, String phone) {
    }
}

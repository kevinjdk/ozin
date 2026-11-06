package com.ozin.infrastructure.identity.adapter.in.rest;

import com.ozin.application.customer.port.in.RegisterCustomerUseCase;
import com.ozin.application.identity.AuthTokens;
import com.ozin.application.identity.port.in.LoginUseCase;
import com.ozin.application.identity.port.in.LogoutUseCase;
import com.ozin.application.identity.port.in.RefreshSessionUseCase;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.Email;
import com.ozin.domain.customer.PhoneNumber;
import com.ozin.domain.identity.RawPassword;
import com.ozin.domain.identity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
class AuthController {

    private final RegisterCustomerUseCase registerCustomer;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;

    AuthController(RegisterCustomerUseCase registerCustomer,
                   LoginUseCase login,
                   RefreshSessionUseCase refreshSession,
                   LogoutUseCase logout) {
        this.registerCustomer = registerCustomer;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente. Los estilistas los crea el administrador")
    CustomerResponse register(@Valid @RequestBody RegisterRequest request) {
        Customer customer = registerCustomer.register(new RegisterCustomerUseCase.Command(request.fullName(),
                new Email(request.email()), new PhoneNumber(request.phone()), new RawPassword(request.password())));
        return new CustomerResponse(customer.id().value(), customer.fullName(),
                customer.email().value(), customer.phone().value());
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve los tokens")
    TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return TokenResponse.from(login.login(
                new LoginUseCase.Command(request.email(), request.password(), http.getRemoteAddr())));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva la sesión; cada token de renovación sirve una sola vez")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshSession.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra la sesión")
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.logout(request.refreshToken());
    }

    record RegisterRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 254) String email,
            @NotBlank String phone,
            @NotBlank String password) {

        @Override
        public String toString() {
            return "RegisterRequest[***]";
        }
    }

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 1000) String password) {

        @Override
        public String toString() {
            return "LoginRequest[***]";
        }
    }

    record RefreshTokenRequest(@NotBlank @Size(max = 200) String refreshToken) {

        @Override
        public String toString() {
            return "RefreshTokenRequest[***]";
        }
    }

    record CustomerResponse(UUID id, String fullName, String email, String phone) {
    }

    record TokenResponse(String accessToken, long expiresIn, String refreshToken, Role role, UUID profileId) {

        static TokenResponse from(AuthTokens tokens) {
            return new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresInSeconds(),
                    tokens.refreshToken(), tokens.role(), tokens.profileId());
        }
    }
}

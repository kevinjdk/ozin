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
import com.ozin.domain.shared.AccessDeniedException;
import com.ozin.infrastructure.config.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * Registro y sesiones (ADR-0008).
 *
 * <p>El token de renovación viaja en una cookie {@code HttpOnly}, inaccesible para JavaScript.
 * Como el navegador envía las cookies por sí solo, las rutas que la usan exigen además la cabecera
 * {@value #CSRF_HEADER}: otro sitio no puede añadirla sin pasar por CORS, que lo rechaza.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
class AuthController {

    static final String REFRESH_COOKIE = "ozin_refresh";
    static final String CSRF_HEADER = "X-Ozin-Client";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final RegisterCustomerUseCase registerCustomer;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final SecurityProperties properties;

    AuthController(RegisterCustomerUseCase registerCustomer,
                   LoginUseCase login,
                   RefreshSessionUseCase refreshSession,
                   LogoutUseCase logout,
                   SecurityProperties properties) {
        this.registerCustomer = registerCustomer;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.properties = properties;
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
    @Operation(summary = "Inicia sesión; devuelve el access token y deja el de renovación en una cookie HttpOnly")
    TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        return openSession(login.login(
                new LoginUseCase.Command(request.email(), request.password(), http.getRemoteAddr())), response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva la sesión con la cookie; cada token de renovación sirve una sola vez")
    TokenResponse refresh(@Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken,
                          @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader,
                          HttpServletResponse response) {
        requireCsrfHeader(csrfHeader);
        return openSession(refreshSession.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra la sesión y elimina la cookie")
    void logout(@Parameter(hidden = true) @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken,
                @RequestHeader(name = CSRF_HEADER, required = false) String csrfHeader,
                HttpServletResponse response) {
        requireCsrfHeader(csrfHeader);
        logout.logout(refreshToken);
        writeCookie(response, "", Duration.ZERO);
    }

    private static void requireCsrfHeader(String csrfHeader) {
        if (csrfHeader == null || csrfHeader.isBlank()) {
            throw new AccessDeniedException("Falta la cabecera " + CSRF_HEADER);
        }
    }

    private TokenResponse openSession(AuthTokens tokens, HttpServletResponse response) {
        writeCookie(response, tokens.refreshToken(), properties.refreshTokenTtl());
        return new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresInSeconds(),
                tokens.role(), tokens.profileId());
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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

    record CustomerResponse(UUID id, String fullName, String email, String phone) {
    }

    record TokenResponse(String accessToken, long expiresIn, Role role, UUID profileId) {
    }
}

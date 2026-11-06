package com.ozin.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Configuración de seguridad ({@code ozin.security.*}). Si falta el secreto, la aplicación no arranca:
 * es preferible fallar al inicio que ejecutarse con una clave conocida.
 *
 * @param jwtSecret     clave de firma HS256; al menos 32 caracteres
 * @param refreshCookieSecure si la cookie de sesión solo viaja por HTTPS; desactivar únicamente en desarrollo
 * @param adminEmail    administrador inicial; si está vacío no se crea ninguno
 * @param adminPassword contraseña del administrador inicial
 */
@Validated
@ConfigurationProperties("ozin.security")
public record SecurityProperties(
        @NotBlank @Size(min = 32) String jwtSecret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @Min(4) int bcryptStrength,
        @Min(1) int maxFailedLogins,
        @NotNull Duration failedLoginWindow,
        @NotEmpty List<String> allowedOrigins,
        boolean refreshCookieSecure,
        String adminEmail,
        String adminPassword) {

    public static final String ROLE_CLAIM = "role";
    public static final String PROFILE_CLAIM = "profileId";

    @Override
    public String toString() {
        return "SecurityProperties[issuer=" + issuer + "]";
    }
}

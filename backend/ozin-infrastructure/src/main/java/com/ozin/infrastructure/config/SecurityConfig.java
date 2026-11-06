package com.ozin.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

/**
 * Autenticación y permisos por ruta (ADR-0004).
 *
 * <p>Todo lo que no aparece aquí queda denegado: un endpoint nuevo sin regla no puede quedar abierto por descuido.
 * Los permisos que dependen de a quién pertenece un recurso se comprueban en los casos de uso.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String STYLIST = "STYLIST";
    private static final String CUSTOMER = "CUSTOMER";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // La API usa tokens Bearer, no cookies de sesión.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/services/manage", "/api/v1/stylists/manage").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/branches", "/api/v1/stylists",
                                "/api/v1/stylists/*/availability").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/branches/*/agenda").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/stylists/*/agenda").hasAnyRole(ADMIN, STYLIST)
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/me/appointments").hasRole(CUSTOMER)

                        .requestMatchers(HttpMethod.POST, "/api/v1/services", "/api/v1/stylists",
                                "/api/v1/stylists/*/time-offs").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/services/*", "/api/v1/stylists/*").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasRole(CUSTOMER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/cancel").hasAnyRole(CUSTOMER, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/confirm",
                                "/api/v1/appointments/*/complete", "/api/v1/appointments/*/no-show").hasAnyRole(STYLIST, ADMIN)

                        .anyRequest().denyAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED",
                                    "Debes iniciar sesión para acceder a este recurso");
                        })
                        .accessDeniedHandler((request, response, ex) -> writeProblem(response,
                                HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED",
                                "No tienes permiso para acceder a este recurso")))
                .build();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(SecurityProperties.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Se escribe a mano porque estos errores ocurren en los filtros, antes de llegar al controlador. */
    private static void writeProblem(HttpServletResponse response, int status, String code, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"status":%d,"code":"%s","detail":"%s"}""".formatted(status, code, detail));
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(java.time.Duration.ZERO);
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(properties.issuer())));
        return decoder;
    }

    private static SecretKey signingKey(SecurityProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

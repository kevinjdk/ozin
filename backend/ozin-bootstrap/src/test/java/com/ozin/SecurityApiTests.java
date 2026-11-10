package com.ozin;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba la seguridad desde el punto de vista de quien intenta saltársela. */
class SecurityApiTests extends ApiTestSupport {

    private static final String MONDAY = "2026-11-09";
    private static final String NEXT_MONDAY = "2026-11-16";
    private static final String REFRESH_COOKIE = "ozin_refresh";
    private static final String CSRF_HEADER = "X-Ozin-Client";

    private String haircut;
    private TestStylist ana;
    private TestCustomer customer;

    @BeforeEach
    void givenASalon() throws Exception {
        haircut = createService("Corte", 45, "12.00");
        ana = createStylist(haircut);
        customer = createCustomer();
    }

    private String refreshCookieOf(String email) throws Exception {
        return login(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(REFRESH_COOKIE, refreshToken))
                .header(CSRF_HEADER, "web"));
    }

    private static String rotatedCookie(ResultActions result) {
        return result.andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();
    }

    private ResultActions loginFrom(String address, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    @Nested
    class RouteAccess {

        @ParameterizedTest(name = "{0} {1}")
        @CsvSource({
                "GET,  /api/v1/services/manage",
                "GET,  /api/v1/stylists/manage",
                "GET,  /api/v1/customers/me/appointments",
                "GET,  /api/v1/branches/00000000-0000-0000-0000-000000000001/agenda",
                "POST, /api/v1/services",
                "POST, /api/v1/stylists",
                "POST, /api/v1/appointments",
                "POST, /api/v1/appointments/00000000-0000-0000-0000-000000000001/cancel",
                "POST, /api/v1/appointments/00000000-0000-0000-0000-000000000001/confirm",
                "GET,  /api/v1/ruta-inexistente",
                "GET,  /actuator/env"
        })
        void visitorsGet401WithAProblemBody(String method, String path) throws Exception {
            mockMvc.perform(request(HttpMethod.valueOf(method), path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void publicRoutesWorkWithoutAToken() throws Exception {
            mockMvc.perform(get("/api/v1/services")).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/branches")).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/stylists")).andExpect(status().isOk());
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }

        @Test
        void customersCannotUseAdminRoutes() throws Exception {
            postJson("/api/v1/services", """
                    {"name": "Gratis", "durationMinutes": 15, "price": 0}
                    """, customer.token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            getWith(customer.token(), "/api/v1/stylists/manage").andExpect(status().isForbidden());
        }

        @Test
        void stylistsCannotUseAdminRoutesNorBook() throws Exception {
            putJson("/api/v1/services/" + haircut, """
                    {"name": "Corte", "durationMinutes": 45, "price": 1, "active": true}
                    """, ana.token()).andExpect(status().isForbidden());
            postJson("/api/v1/appointments", """
                    {"stylistId": "%s", "serviceId": "%s", "start": "%s"}
                    """.formatted(ana.id(), haircut, at(MONDAY + "T10:00")), ana.token())
                    .andExpect(status().isForbidden());
        }

        @Test
        void publicStylistListDoesNotExposeTimeOffOrWorkingDetails() throws Exception {
            postJson("/api/v1/stylists/" + ana.id() + "/time-offs", """
                    {"start": "%s", "end": "%s", "reason": "Cita médica"}
                    """.formatted(at(MONDAY + "T09:00"), at(MONDAY + "T10:00")), adminToken)
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/stylists"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Cita médica"))));
        }
    }

    @Nested
    class Ownership {

        @Test
        void bookingAlwaysUsesTheAuthenticatedCustomerEvenIfTheBodySaysOtherwise() throws Exception {
            TestCustomer victim = createCustomer();

            postJson("/api/v1/appointments", """
                    {"customerId": "%s", "stylistId": "%s", "serviceId": "%s", "start": "%s"}
                    """.formatted(victim.id(), ana.id(), haircut, at(MONDAY + "T10:00")), customer.token())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value(customer.id()));

            getWith(victim.token(), "/api/v1/customers/me/appointments").andExpect(jsonPath("$.items", hasSize(0)));
        }

        @Test
        void aCustomerCannotCancelAnotherCustomersAppointment() throws Exception {
            String appointment = createdId(book(customer, ana.id(), haircut, NEXT_MONDAY + "T10:00"));

            postJson("/api/v1/appointments/" + appointment + "/cancel", "", createCustomer().token())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            getWith(customer.token(), "/api/v1/customers/me/appointments")
                    .andExpect(jsonPath("$.items[0].status").value("PENDING"));
        }

        @Test
        void aCustomerOnlySeesTheirOwnAppointments() throws Exception {
            createdId(book(customer, ana.id(), haircut, MONDAY + "T10:00"));

            getWith(createCustomer().token(), "/api/v1/customers/me/appointments")
                    .andExpect(jsonPath("$.totalItems").value(0));
        }

        @Test
        void aCustomerCannotManageAppointmentStates() throws Exception {
            String appointment = createdId(book(customer, ana.id(), haircut, MONDAY + "T10:00"));

            postJson("/api/v1/appointments/" + appointment + "/confirm", "", customer.token())
                    .andExpect(status().isForbidden());
        }

        @Test
        void aStylistCannotManageAnotherStylistsAppointment() throws Exception {
            String appointment = createdId(book(customer, ana.id(), haircut, MONDAY + "T10:00"));

            postJson("/api/v1/appointments/" + appointment + "/confirm", "", createStylist(haircut).token())
                    .andExpect(status().isForbidden());
        }

        @Test
        void aStylistCannotCancelAppointments() throws Exception {
            String appointment = createdId(book(customer, ana.id(), haircut, NEXT_MONDAY + "T10:00"));

            postJson("/api/v1/appointments/" + appointment + "/cancel", "", ana.token())
                    .andExpect(status().isForbidden());
        }

        @Test
        void aStylistCannotSeeAnotherStylistsAgenda() throws Exception {
            getWith(createStylist(haircut).token(), get("/api/v1/stylists/{id}/agenda", ana.id())
                    .param("from", at(MONDAY + "T00:00").toString())
                    .param("to", at("2026-11-10T00:00").toString()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Registration {

        private String body(String email, String password) {
            return """
                    {"fullName": "Eva", "email": "%s", "phone": "+593991234567", "password": "%s"}
                    """.formatted(email, password);
        }

        @Test
        void hu06_passwordIsStoredHashedAndNeverReturned() throws Exception {
            postJson("/api/v1/auth/register", body("eva@example.com", PASSWORD))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(not(containsString(PASSWORD))))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());

            String stored = jdbc.queryForObject(
                    "SELECT password_hash FROM app_user WHERE email = 'eva@example.com'", String.class);
            assertThat(stored).startsWith("$2").doesNotContain(PASSWORD);
        }

        @Test
        void hu06_rejectsWeakPasswords() throws Exception {
            postJson("/api/v1/auth/register", body("eva@example.com", "debil"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void rejectsDuplicateEmailIgnoringCase() throws Exception {
            postJson("/api/v1/auth/register", body("eva@example.com", PASSWORD)).andExpect(status().isCreated());

            postJson("/api/v1/auth/register", body("EVA@example.com", PASSWORD))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        }

        @Test
        void rejectsInvalidPhoneNumber() throws Exception {
            postJson("/api/v1/auth/register", """
                    {"fullName": "Eva", "email": "eva@example.com", "phone": "0991234567", "password": "%s"}
                    """.formatted(PASSWORD)).andExpect(status().isBadRequest());
        }

        @Test
        void nobodyCanRegisterThemselvesAsAdmin() throws Exception {
            postJson("/api/v1/auth/register", """
                    {"fullName": "Eva", "email": "eva@example.com", "phone": "+593991234567",
                     "password": "%s", "role": "ADMIN"}
                    """.formatted(PASSWORD)).andExpect(status().isCreated());

            login("eva@example.com", PASSWORD).andExpect(jsonPath("$.role").value("CUSTOMER"));
        }
    }

    @Nested
    class Login {

        @Test
        void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
            String wrongPassword = login(ADMIN_EMAIL, "Incorrecta1")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andReturn().getResponse().getContentAsString();

            String unknownEmail = login("nadie@example.com", "Incorrecta1")
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();

            assertThat(unknownEmail).isEqualTo(wrongPassword);
        }

        @Test
        void passwordsLongerThanBcryptAllowsAreRejectedWithoutError() throws Exception {
            login(ADMIN_EMAIL, PASSWORD + "x".repeat(100)).andExpect(status().isUnauthorized());
        }

        @Test
        void blocksAfterFiveFailuresEvenWithTheRightPassword() throws Exception {
            String victim = "bloqueo-1@example.com";
            registerUser(victim);
            for (int i = 0; i < 5; i++) {
                loginFrom("10.0.0.1", victim, "Incorrecta1").andExpect(status().isUnauthorized());
            }

            loginFrom("10.0.0.1", victim, PASSWORD)
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
        }

        @Test
        void anAttackerCannotLockTheRealUserOutFromAnotherAddress() throws Exception {
            String victim = "bloqueo-2@example.com";
            registerUser(victim);
            for (int i = 0; i < 5; i++) {
                loginFrom("10.0.0.2", victim, "Incorrecta1").andExpect(status().isUnauthorized());
            }

            loginFrom("10.0.0.3", victim, PASSWORD).andExpect(status().isOk());
        }

        @Test
        void theBlockExpiresAfterTheWindow() throws Exception {
            String victim = "bloqueo-3@example.com";
            registerUser(victim);
            for (int i = 0; i < 5; i++) {
                loginFrom("10.0.0.4", victim, "Incorrecta1").andExpect(status().isUnauthorized());
            }
            clock.set(at(MONDAY + "T08:15"));

            loginFrom("10.0.0.4", victim, PASSWORD).andExpect(status().isOk());
        }

        @Test
        void aSuccessfulLoginResetsTheFailureCount() throws Exception {
            String user = "bloqueo-4@example.com";
            registerUser(user);
            for (int i = 0; i < 4; i++) {
                loginFrom("10.0.0.5", user, "Incorrecta1").andExpect(status().isUnauthorized());
            }
            loginFrom("10.0.0.5", user, PASSWORD).andExpect(status().isOk());
            loginFrom("10.0.0.5", user, "Incorrecta1").andExpect(status().isUnauthorized());

            loginFrom("10.0.0.5", user, PASSWORD).andExpect(status().isOk());
        }

        private void registerUser(String email) throws Exception {
            postJson("/api/v1/auth/register", """
                    {"fullName": "Eva", "email": "%s", "phone": "+593991234567", "password": "%s"}
                    """.formatted(email, PASSWORD)).andExpect(status().isCreated());
        }
    }

    @Nested
    class AccessTokens {

        @Test
        void expiredTokensAreRejected() throws Exception {
            clock.set(at("2026-12-10T08:00"));

            getWith(adminToken, "/api/v1/stylists/manage").andExpect(status().isUnauthorized());
        }

        @Test
        void tamperedTokensAreRejected() throws Exception {
            String[] parts = customer.token().split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                    .replace("CUSTOMER", "ADMIN");
            String forged = parts[0] + "."
                    + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes())
                    + "." + parts[2];

            getWith(forged, "/api/v1/stylists/manage").andExpect(status().isUnauthorized());
        }

        @Test
        void unsignedTokensAreRejected() throws Exception {
            String header = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"none\"}".getBytes());
            String payload = customer.token().split("\\.")[1];

            getWith(header + "." + payload + ".", "/api/v1/customers/me/appointments")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RefreshTokens {

        @Test
        void rotatesTheTokenAndKeepsTheSessionAlive() throws Exception {
            String first = refreshCookieOf(ADMIN_EMAIL);

            String second = rotatedCookie(refresh(first).andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN")));

            assertThat(second).isNotEqualTo(first);
            refresh(second).andExpect(status().isOk());
        }

        @Test
        void onlyTheHashIsStored() throws Exception {
            String raw = refreshCookieOf(ADMIN_EMAIL);

            Integer plainCopies = jdbc.queryForObject(
                    "SELECT count(*) FROM refresh_token WHERE token_hash = ?", Integer.class, raw);

            assertThat(plainCopies).isZero();
        }

        @Test
        void reusingAnOldTokenRevokesTheWholeSession() throws Exception {
            String stolen = refreshCookieOf(ADMIN_EMAIL);
            String current = rotatedCookie(refresh(stolen).andExpect(status().isOk()));

            refresh(stolen)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

            refresh(current).andExpect(status().isUnauthorized());
        }

        @Test
        void reuseOnlyRevokesThatSessionNotOtherDevices() throws Exception {
            String stolen = refreshCookieOf(ADMIN_EMAIL);
            String otherDevice = refreshCookieOf(ADMIN_EMAIL);
            refresh(stolen).andExpect(status().isOk());

            refresh(stolen).andExpect(status().isUnauthorized());

            refresh(otherDevice).andExpect(status().isOk());
        }

        @Test
        void expiredTokensAreRejected() throws Exception {
            String token = refreshCookieOf(ADMIN_EMAIL);
            clock.set(at("2026-11-16T08:00"));

            refresh(token).andExpect(status().isUnauthorized());
        }

        @Test
        void unknownTokensAreRejected() throws Exception {
            refresh("token-inventado")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        void logoutEndsTheSession() throws Exception {
            String token = refreshCookieOf(ADMIN_EMAIL);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie(REFRESH_COOKIE, token))
                            .header(CSRF_HEADER, "web"))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

            refresh(token).andExpect(status().isUnauthorized());
        }

        @Test
        void aRequestWithoutCookieIsRejected() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh").header(CSRF_HEADER, "web"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }
    }

    @Nested
    class SessionCookie {

        @Test
        void isInaccessibleToJavascriptAndRestrictedToTheAuthRoutes() throws Exception {
            login(ADMIN_EMAIL, PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").doesNotExist())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=Strict"),
                            containsString("Path=/api/v1/auth"))));
        }

        @Test
        void csrf_refreshIsRejectedWithoutTheCustomHeader() throws Exception {
            String token = refreshCookieOf(ADMIN_EMAIL);

            mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, token)))
                    .andExpect(status().isForbidden());

            refresh(token).andExpect(status().isOk());
        }

        @Test
        void csrf_logoutIsRejectedWithoutTheCustomHeader() throws Exception {
            String token = refreshCookieOf(ADMIN_EMAIL);

            mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(REFRESH_COOKIE, token)))
                    .andExpect(status().isForbidden());

            refresh(token).andExpect(status().isOk());
        }

        @Test
        void cors_otherOriginsCannotSendTheCustomHeader() throws Exception {
            mockMvc.perform(options("/api/v1/auth/refresh")
                            .header(HttpHeaders.ORIGIN, "https://sitio-malicioso.example")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CSRF_HEADER))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DeactivatedStylist {

        private void deactivateAna() throws Exception {
            putJson("/api/v1/stylists/" + ana.id(), """
                    {"fullName": "Ana Pérez", "active": false, "serviceIds": ["%s"], "workingHours": %s}
                    """.formatted(haircut, mondayShifts()), adminToken).andExpect(status().isOk());
        }

        @Test
        void cannotLogIn() throws Exception {
            deactivateAna();

            login(ana.email(), PASSWORD).andExpect(status().isUnauthorized());
        }

        @Test
        void cannotRenewAnOpenSession() throws Exception {
            String token = refreshCookieOf(ana.email());
            deactivateAna();

            refresh(token).andExpect(status().isUnauthorized());
        }

        @Test
        void recoversAccessWhenReactivated() throws Exception {
            deactivateAna();
            putJson("/api/v1/stylists/" + ana.id(), """
                    {"fullName": "Ana Pérez", "active": true, "serviceIds": ["%s"], "workingHours": %s}
                    """.formatted(haircut, mondayShifts()), adminToken).andExpect(status().isOk());

            login(ana.email(), PASSWORD).andExpect(status().isOk());
        }
    }

    @Nested
    class Cors {

        private ResultActions preflightFrom(String origin) throws Exception {
            return mockMvc.perform(options("/api/v1/services")
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));
        }

        @Test
        void allowsTheConfiguredFrontend() throws Exception {
            preflightFrom("http://localhost:4200")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        }

        @Test
        void rejectsOtherOrigins() throws Exception {
            preflightFrom("https://sitio-malicioso.example").andExpect(status().isForbidden());
        }
    }
}

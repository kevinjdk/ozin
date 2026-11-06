package com.ozin;

import com.jayway.jsonpath.JsonPath;
import com.ozin.application.identity.port.in.EnsureAdminUseCase;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de la API: aplicación completa contra un PostgreSQL real, con tokens reales.
 *
 * <p>Escenario: lunes 9 de noviembre de 2026, 08:00 en Ecuador. La estilista creada por
 * {@link #createStylist} trabaja los lunes de 09:00 a 13:00 y de 14:00 a 18:00.
 *
 * <p>BCrypt usa el coste mínimo para que los tests sean rápidos, y el access token dura 30 días para
 * que mover el reloj no lo invalide; la caducidad se prueba expresamente en {@code SecurityApiTests}.
 */
@SpringBootTest(properties = {
        "ozin.security.jwt-secret=" + ApiTestSupport.JWT_SECRET,
        "ozin.security.bcrypt-strength=4",
        "ozin.security.access-token-ttl=30d"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiTestSupport.ClockConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public abstract class ApiTestSupport {

    static final String JWT_SECRET = "solo-para-tests-no-es-un-secreto-real-0123456789";

    protected static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    protected static final String DEFAULT_BRANCH = "00000000-0000-0000-0000-000000000001";
    protected static final String PASSWORD = "Segura123";
    protected static final String ADMIN_EMAIL = "admin@example.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private EnsureAdminUseCase ensureAdmin;

    protected String adminToken;
    private int customerSequence;
    private int stylistSequence;

    protected record TestCustomer(String id, String token) {
    }

    protected record TestStylist(String id, String email, String token) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.EPOCH);
        }
    }

    @BeforeEach
    void resetState() throws Exception {
        jdbc.execute("TRUNCATE refresh_token, app_user, appointment, time_off, working_hours, stylist_service, "
                + "stylist, customer, salon_service");
        clock.set(at("2026-11-09T08:00"));
        ensureAdmin.ensureAdmin(new Email(ADMIN_EMAIL), new RawPassword(PASSWORD));
        adminToken = accessTokenOf(ADMIN_EMAIL);
    }

    protected static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
    }

    protected ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions postJson(String path, String body, String token) throws Exception {
        return mockMvc.perform(authorized(post(path), token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String path, String body, String token) throws Exception {
        return mockMvc.perform(authorized(put(path), token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getWith(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(authorized(request, token));
    }

    protected ResultActions getWith(String token, String path) throws Exception {
        return getWith(token, get(path));
    }

    protected static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected ResultActions login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    protected String accessTokenOf(String email) throws Exception {
        String body = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected String createService(String name, int durationMinutes, String price) throws Exception {
        return createdId(postJson("/api/v1/services", """
                {"name": "%s", "durationMinutes": %d, "price": %s}
                """.formatted(name, durationMinutes, price), adminToken));
    }

    protected TestStylist createStylist(String serviceId) throws Exception {
        stylistSequence++;
        String email = "estilista%d@example.com".formatted(stylistSequence);
        String id = createdId(postJson("/api/v1/stylists", """
                {"branchId": "%s", "fullName": "Ana Pérez", "email": "%s", "password": "%s",
                 "serviceIds": ["%s"], "workingHours": %s}
                """.formatted(DEFAULT_BRANCH, email, PASSWORD, serviceId, mondayShifts()), adminToken));
        return new TestStylist(id, email, accessTokenOf(email));
    }

    protected static String mondayShifts() {
        return """
                [{"day": "MONDAY", "start": "09:00", "end": "13:00"},
                 {"day": "MONDAY", "start": "14:00", "end": "18:00"}]
                """;
    }

    protected TestCustomer createCustomer() throws Exception {
        customerSequence++;
        String email = "cliente%d@example.com".formatted(customerSequence);
        String id = createdId(postJson("/api/v1/auth/register", """
                {"fullName": "Cliente %d", "email": "%s", "phone": "+593991234567", "password": "%s"}
                """.formatted(customerSequence, email, PASSWORD)));
        return new TestCustomer(id, accessTokenOf(email));
    }

    protected ResultActions book(TestCustomer customer, String stylistId, String serviceId, String localStart)
            throws Exception {
        return postJson("/api/v1/appointments", """
                {"stylistId": "%s", "serviceId": "%s", "start": "%s"}
                """.formatted(stylistId, serviceId, at(localStart)), customer.token());
    }

    protected static String createdId(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}

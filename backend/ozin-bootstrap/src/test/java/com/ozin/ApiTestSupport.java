package com.ozin;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de la API: aplicación completa contra un PostgreSQL real.
 *
 * <p>Escenario: lunes 9 de noviembre de 2026, 08:00 en Ecuador. La estilista creada por
 * {@link #createStylist} trabaja los lunes de 09:00 a 13:00 y de 14:00 a 18:00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiTestSupport.ClockConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public abstract class ApiTestSupport {

    protected static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    protected static final String DEFAULT_BRANCH = "00000000-0000-0000-0000-000000000001";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected JdbcTemplate jdbc;

    private int customerSequence;

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.EPOCH);
        }
    }

    @BeforeEach
    void resetState() {
        jdbc.execute("TRUNCATE appointment, time_off, working_hours, stylist_service, stylist, customer, salon_service");
        clock.set(at("2026-11-09T08:00"));
    }

    protected static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
    }

    protected ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String path, String body) throws Exception {
        return mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected String createService(String name, int durationMinutes, String price) throws Exception {
        return createdId(postJson("/api/v1/services", """
                {"name": "%s", "durationMinutes": %d, "price": %s}
                """.formatted(name, durationMinutes, price)));
    }

    protected String createStylist(String serviceId) throws Exception {
        return createdId(postJson("/api/v1/stylists", """
                {"branchId": "%s", "fullName": "Ana Pérez", "serviceIds": ["%s"], "workingHours": %s}
                """.formatted(DEFAULT_BRANCH, serviceId, mondayShifts())));
    }

    protected static String mondayShifts() {
        return """
                [{"day": "MONDAY", "start": "09:00", "end": "13:00"},
                 {"day": "MONDAY", "start": "14:00", "end": "18:00"}]
                """;
    }

    protected String createCustomer() throws Exception {
        customerSequence++;
        return createdId(postJson("/api/v1/customers", """
                {"fullName": "Cliente %d", "email": "cliente%d@example.com", "phone": "+593991234567"}
                """.formatted(customerSequence, customerSequence)));
    }

    protected ResultActions book(String customerId, String stylistId, String serviceId, String localStart)
            throws Exception {
        return postJson("/api/v1/appointments", """
                {"customerId": "%s", "stylistId": "%s", "serviceId": "%s", "start": "%s"}
                """.formatted(customerId, stylistId, serviceId, at(localStart)));
    }

    protected static String createdId(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}

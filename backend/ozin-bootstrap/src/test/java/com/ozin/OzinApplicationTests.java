package com.ozin;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba el arranque completo: migraciones Flyway, validación del esquema y endpoints técnicos. */
class OzinApplicationTests extends ApiTestSupport {

    @Test
    void contextLoadsAndHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void publishesTheOpenApiContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ozin API"))
                .andExpect(jsonPath("$.paths['/api/v1/appointments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stylists/{stylistId}/availability'].get").exists());
    }
}

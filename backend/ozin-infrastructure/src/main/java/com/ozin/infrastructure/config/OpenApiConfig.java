package com.ozin.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI ozinOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ozin API")
                .description("API REST del sistema de reservas para peluquerías Ozin")
                .version("v1")
                .license(new License().name("MIT")));
    }
}

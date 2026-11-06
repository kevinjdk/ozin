package com.ozin;

import org.springframework.boot.SpringApplication;

import java.util.Arrays;
import java.util.stream.Stream;

/** Arranca la aplicación localmente con un PostgreSQL en Testcontainers (sin docker-compose). */
public class TestOzinApplication {

    public static void main(String[] args) {
        String[] withSecret = Stream.concat(Arrays.stream(args),
                Stream.of("--ozin.security.jwt-secret=" + ApiTestSupport.JWT_SECRET)).toArray(String[]::new);
        SpringApplication.from(OzinApplication::main).with(TestcontainersConfiguration.class).run(withSecret);
    }
}

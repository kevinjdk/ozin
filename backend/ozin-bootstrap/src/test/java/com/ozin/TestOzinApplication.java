package com.ozin;

import org.springframework.boot.SpringApplication;

/** Arranca la aplicación localmente con un PostgreSQL en Testcontainers (sin docker-compose). */
public class TestOzinApplication {

    public static void main(String[] args) {
        SpringApplication.from(OzinApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}

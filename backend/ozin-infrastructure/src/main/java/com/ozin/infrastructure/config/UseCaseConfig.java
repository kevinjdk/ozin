package com.ozin.infrastructure.config;

import com.ozin.domain.booking.AppointmentBooker;
import com.ozin.domain.booking.BookingPolicy;
import com.ozin.domain.booking.CancellationPolicy;
import com.ozin.domain.booking.StandardCancellationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Clock;

/**
 * Registra como beans las clases sin anotaciones de las capas interiores.
 *
 * <p>Los casos de uso se detectan por convención de paquete y nombre, de modo que la capa
 * de aplicación no necesita ninguna anotación de Spring.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(
        basePackages = "com.ozin.application",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.ozin\\.application\\..+\\.service\\..+Service"))
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AppointmentBooker appointmentBooker() {
        return new AppointmentBooker(BookingPolicy.standard());
    }

    @Bean
    CancellationPolicy cancellationPolicy() {
        return StandardCancellationPolicy.withDefaults();
    }
}

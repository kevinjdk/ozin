package com.ozin.application.catalog.port.in;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.shared.Money;

import java.time.Duration;

/** HU-10. */
public interface CreateSalonServiceUseCase {

    SalonService create(Command command);

    record Command(String name, Duration duration, Money price) {
    }
}

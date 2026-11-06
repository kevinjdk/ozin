package com.ozin.application.catalog.port.in;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.Money;

import java.time.Duration;

/** HU-10. */
public interface UpdateSalonServiceUseCase {

    SalonService update(Command command);

    record Command(SalonServiceId id, String name, Duration duration, Money price, boolean active) {
    }
}

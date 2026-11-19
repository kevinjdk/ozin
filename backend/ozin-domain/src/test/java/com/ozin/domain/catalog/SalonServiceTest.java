package com.ozin.domain.catalog;

import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalonServiceTest {

    private static SalonService withDescription(String description) {
        return new SalonService(SalonServiceId.newId(), "Corte", description, Duration.ofMinutes(30),
                Money.usd("12.00"), true);
    }

    @Test
    void descriptionIsOptionalAndNeverNull() {
        assertThat(withDescription(null).description()).isEmpty();
    }

    @Test
    void descriptionIsTrimmed() {
        assertThat(withDescription("  Lavado, corte y peinado.  ").description()).isEqualTo("Lavado, corte y peinado.");
    }

    @Test
    void rejectsDescriptionsLongerThanTheLimit() {
        String tooLong = "a".repeat(SalonService.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> withDescription(tooLong)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reviseUpdatesTheDescription() {
        SalonService service = withDescription("Antes");

        service.revise("Corte", "Después", Duration.ofMinutes(30), Money.usd("12.00"), true);

        assertThat(service.description()).isEqualTo("Después");
    }
}

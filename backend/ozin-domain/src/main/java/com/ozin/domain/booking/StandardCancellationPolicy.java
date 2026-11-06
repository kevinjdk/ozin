package com.ozin.domain.booking;

import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.DomainValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * RN-07: el cliente puede cancelar solo con una anticipación mínima; el administrador, siempre.
 */
public final class StandardCancellationPolicy implements CancellationPolicy {

    public static final Duration DEFAULT_MINIMUM_NOTICE = Duration.ofHours(24);

    private final Duration minimumNotice;

    public StandardCancellationPolicy(Duration minimumNotice) {
        Objects.requireNonNull(minimumNotice, "minimumNotice");
        if (minimumNotice.isNegative()) {
            throw new DomainValidationException("La anticipación mínima no puede ser negativa");
        }
        this.minimumNotice = minimumNotice;
    }

    public static StandardCancellationPolicy withDefaults() {
        return new StandardCancellationPolicy(DEFAULT_MINIMUM_NOTICE);
    }

    @Override
    public void ensureCanCancel(Appointment appointment, CancellationActor actor, Instant now) {
        if (actor == CancellationActor.ADMIN) {
            return;
        }
        Instant deadline = appointment.slot().start().minus(minimumNotice);
        if (now.isAfter(deadline)) {
            throw new BusinessRuleViolationException("RN-07",
                    "Solo puedes cancelar hasta %d horas antes de la cita. Comunícate con el salón."
                            .formatted(minimumNotice.toHours()));
        }
    }
}

package com.ozin.domain.catalog;

import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.shared.Money;

import java.time.Duration;
import java.util.Objects;

/**
 * Servicio ofrecido por el salón (corte, tinte, barba...).
 *
 * <p>Se llama {@code SalonService} para no confundirlo con {@code @Service} de Spring.
 */
public final class SalonService {

    private static final Duration MAX_DURATION = Duration.ofHours(8);

    private final SalonServiceId id;
    private String name;
    private Duration duration;
    private Money price;
    private boolean active;

    public SalonService(SalonServiceId id, String name, Duration duration, Money price, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireName(name);
        this.duration = requireDuration(duration);
        this.price = Objects.requireNonNull(price, "price");
        this.active = active;
    }

    /** Cambiar el precio no afecta a las citas ya reservadas, que lo tienen congelado (RN-09). */
    public void revise(String name, Duration duration, Money price, boolean active) {
        this.name = requireName(name);
        this.duration = requireDuration(duration);
        this.price = Objects.requireNonNull(price, "price");
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    /** Desactivar en lugar de borrar conserva el histórico de citas que referencian el servicio. */
    public void deactivate() {
        this.active = false;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("El nombre del servicio es obligatorio");
        }
        return name.strip();
    }

    private static Duration requireDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero() || duration.compareTo(MAX_DURATION) > 0) {
            throw new DomainValidationException("La duración del servicio debe estar entre 1 minuto y 8 horas");
        }
        return duration;
    }

    public SalonServiceId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Duration duration() {
        return duration;
    }

    public Money price() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SalonService other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

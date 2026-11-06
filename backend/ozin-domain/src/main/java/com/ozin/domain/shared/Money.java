package com.ozin.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Cantidad monetaria inmutable (RN-14).
 *
 * <p>Usa {@link BigDecimal} con la escala de la moneda; nunca {@code double}.
 * Rechaza montos con más decimales de los que admite la moneda en lugar de redondearlos
 * en silencio, para no perder precisión sin que nadie lo note.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency USD = Currency.getInstance("USD");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new DomainValidationException("El monto no puede ser negativo");
        }
        int fractionDigits = currency.getDefaultFractionDigits();
        if (amount.stripTrailingZeros().scale() > fractionDigits) {
            throw new DomainValidationException(
                    "El monto admite como máximo %d decimales".formatted(fractionDigits));
        }
        amount = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
    }

    public static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, USD);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainValidationException(
                    "No se pueden operar montos de monedas distintas: %s y %s".formatted(currency, other.currency));
        }
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}

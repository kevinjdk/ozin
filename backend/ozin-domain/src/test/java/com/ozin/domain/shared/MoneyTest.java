package com.ozin.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesScaleToCurrencyDecimals() {
        assertThat(Money.usd("12").amount()).isEqualByComparingTo("12.00");
        assertThat(Money.usd("12")).isEqualTo(Money.usd("12.00"));
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> Money.usd("-1.00"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsMorePrecisionThanTheCurrencyAllowsInsteadOfRoundingSilently() {
        assertThatThrownBy(() -> Money.usd("10.005"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("2 decimales");
    }

    @Test
    void addsAmountsOfTheSameCurrency() {
        assertThat(Money.usd("12.50").add(Money.usd("7.25"))).isEqualTo(Money.usd("19.75"));
    }

    @Test
    void rejectsOperationsBetweenDifferentCurrencies() {
        Money euros = new Money(new BigDecimal("5.00"), Currency.getInstance("EUR"));

        assertThatThrownBy(() -> Money.usd("5.00").add(euros))
                .isInstanceOf(DomainValidationException.class);
    }
}

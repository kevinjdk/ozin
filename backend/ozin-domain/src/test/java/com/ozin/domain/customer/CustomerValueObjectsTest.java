package com.ozin.domain.customer;

import com.ozin.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerValueObjectsTest {

    @Test
    void emailIsNormalizedSoUniquenessIgnoresCase() {
        assertThat(new Email("  Ana.Perez@Example.COM ")).isEqualTo(new Email("ana.perez@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "@example.com", "ana@example", "ana perez@example.com"})
    void rejectsInvalidEmails(String value) {
        assertThatThrownBy(() -> new Email(value)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void acceptsE164PhoneNumber() {
        assertThat(new PhoneNumber("+593991234567").value()).isEqualTo("+593991234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0991234567", "+0991234567", "+593 99 123 4567", "+12345"})
    void rejectsPhoneNumbersNotInE164(String value) {
        assertThatThrownBy(() -> new PhoneNumber(value)).isInstanceOf(DomainValidationException.class);
    }
}

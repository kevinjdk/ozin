package com.ozin.domain.identity;

import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.customer.Email;
import com.ozin.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "Corta1", "sinmayuscula1", "SinNumeroAqui"})
    void rejectsPasswordsOutsideThePolicy(String value) {
        assertThatThrownBy(() -> new RawPassword(value)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsPasswordsLongerThanBcryptCanHandle() {
        assertThatThrownBy(() -> new RawPassword("A1" + "x".repeat(71)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void passwordIsNeverPrinted() {
        assertThat(new RawPassword("Secreta123")).asString().doesNotContain("Secreta123");
    }

    @Test
    void onlyAdminsHaveNoProfile() {
        Email email = new Email("ana@example.com");

        assertThatThrownBy(() -> new User(UserId.newId(), email, "hash", Role.CUSTOMER, null, true))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new User(UserId.newId(), email, "hash", Role.ADMIN, UUID.randomUUID(), true))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void actorOnlyMatchesItsOwnProfile() {
        CustomerId own = CustomerId.newId();
        Actor actor = new Actor(Role.CUSTOMER, own.value());

        assertThat(actor.isCustomer(own)).isTrue();
        assertThat(actor.isCustomer(CustomerId.newId())).isFalse();
        assertThat(actor.isAdmin()).isFalse();
    }

    @Test
    void refreshTokenExpiresExactlyAtItsDeadline() {
        Instant deadline = Instant.parse("2026-11-16T00:00:00Z");
        RefreshToken token = new RefreshToken(UUID.randomUUID(), UserId.newId(), UUID.randomUUID(), "h", deadline, null);

        assertThat(token.isExpired(deadline.minusSeconds(1))).isFalse();
        assertThat(token.isExpired(deadline)).isTrue();
        assertThat(token.revoke(deadline).isRevoked()).isTrue();
    }
}

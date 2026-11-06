package com.ozin.domain.booking;

import com.ozin.domain.identity.Actor;
import com.ozin.domain.identity.Role;
import com.ozin.domain.shared.AccessDeniedException;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.InvalidStateTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.UUID;

import static com.ozin.domain.booking.BookingFixtures.appointment;
import static com.ozin.domain.booking.BookingFixtures.at;
import static com.ozin.domain.booking.BookingFixtures.stylist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final String START = "2026-11-09T10:00";
    private final CancellationPolicy policy = StandardCancellationPolicy.withDefaults();

    private static Appointment withStatus(AppointmentStatus status) {
        return appointment(stylist(), START, Duration.ofMinutes(45), status);
    }

    @Nested
    class Confirm {

        @Test
        void pendingAppointmentCanBeConfirmed() {
            Appointment appointment = withStatus(AppointmentStatus.PENDING);

            appointment.confirm();

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
        void onlyPendingAppointmentsCanBeConfirmed(AppointmentStatus status) {
            assertThatThrownBy(() -> withStatus(status).confirm())
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    class Cancel {

        @Test
        void customerCanCancelWithAtLeast24HoursNotice() {
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            appointment.cancel(CancellationActor.CUSTOMER, at("2026-11-08T10:00"), policy);

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment.cancelledBy()).contains(CancellationActor.CUSTOMER);
            assertThat(appointment.isActive()).isFalse();
        }

        @Test
        void customerCannotCancelWithLessThan24HoursNotice() {
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            assertThatThrownBy(() -> appointment.cancel(CancellationActor.CUSTOMER, at("2026-11-08T10:01"), policy))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("code").isEqualTo("RN-07");
            assertThat(appointment.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @Test
        void adminCanCancelAtAnyTime() {
            Appointment appointment = withStatus(AppointmentStatus.PENDING);

            appointment.cancel(CancellationActor.ADMIN, at("2026-11-09T09:55"), policy);

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment.cancelledBy()).contains(CancellationActor.ADMIN);
        }

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
        void finishedAppointmentsCannotBeCancelled(AppointmentStatus status) {
            assertThatThrownBy(() -> withStatus(status).cancel(CancellationActor.ADMIN, at("2026-11-01T10:00"), policy))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void acceptsAlternativePoliciesWithoutChangingTheEntity() {
            CancellationPolicy anytime = (appointment, actor, now) -> { };
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            appointment.cancel(CancellationActor.CUSTOMER, at("2026-11-09T09:59"), anytime);

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        }
    }

    @Nested
    class Authorization {

        private final Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

        @Test
        void adminAndTheAppointmentsStylistCanManageIt() {
            assertThatCode(() -> appointment.ensureManageableBy(new Actor(Role.ADMIN, null)))
                    .doesNotThrowAnyException();
            assertThatCode(() -> appointment.ensureManageableBy(new Actor(Role.STYLIST, appointment.stylistId().value())))
                    .doesNotThrowAnyException();
        }

        @Test
        void anotherStylistCannotManageIt() {
            assertThatThrownBy(() -> appointment.ensureManageableBy(new Actor(Role.STYLIST, UUID.randomUUID())))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void cancellationActorDependsOnWhoAsks() {
            assertThat(appointment.cancellationActorFor(new Actor(Role.ADMIN, null)))
                    .isEqualTo(CancellationActor.ADMIN);
            assertThat(appointment.cancellationActorFor(new Actor(Role.CUSTOMER, appointment.customerId().value())))
                    .isEqualTo(CancellationActor.CUSTOMER);
        }

        @Test
        void neitherOtherCustomersNorStylistsCanCancel() {
            assertThatThrownBy(() -> appointment.cancellationActorFor(new Actor(Role.CUSTOMER, UUID.randomUUID())))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appointment.cancellationActorFor(new Actor(Role.STYLIST, appointment.stylistId().value())))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class CompleteAndNoShow {

        @Test
        void confirmedAppointmentCanBeCompletedOnceStarted() {
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            appointment.complete(at("2026-11-09T10:45"));

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        void cannotBeCompletedBeforeItStarts() {
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            assertThatThrownBy(() -> appointment.complete(at("2026-11-09T09:59")))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void pendingAppointmentCannotBeCompleted() {
            assertThatThrownBy(() -> withStatus(AppointmentStatus.PENDING).complete(at("2026-11-09T11:00")))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void confirmedAppointmentCanBeMarkedAsNoShowOnceStarted() {
            Appointment appointment = withStatus(AppointmentStatus.CONFIRMED);

            appointment.markNoShow(at("2026-11-09T10:15"));

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.NO_SHOW);
        }

        @Test
        void cannotBeMarkedAsNoShowBeforeItStarts() {
            assertThatThrownBy(() -> withStatus(AppointmentStatus.CONFIRMED).markNoShow(at("2026-11-09T09:00")))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }
}

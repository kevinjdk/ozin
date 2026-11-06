package com.ozin.domain.booking;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.Money;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.ozin.domain.booking.BookingFixtures.BRANCH;
import static com.ozin.domain.booking.BookingFixtures.COLORING;
import static com.ozin.domain.booking.BookingFixtures.HAIRCUT;
import static com.ozin.domain.booking.BookingFixtures.MONDAY_SHIFTS;
import static com.ozin.domain.booking.BookingFixtures.ZONE;
import static com.ozin.domain.booking.BookingFixtures.appointment;
import static com.ozin.domain.booking.BookingFixtures.at;
import static com.ozin.domain.booking.BookingFixtures.stylist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentBookerTest {

    private static final Instant NOW = at("2026-11-09T08:00");
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    private final AppointmentBooker booker = new AppointmentBooker(BookingPolicy.standard());
    private final CustomerId customer = CustomerId.newId();
    private final Stylist ana = stylist();

    private Appointment book(Stylist stylist, SalonService service, String start,
                             Collection<Appointment> existing, long customerActive, Instant now) {
        return booker.book(new BookingRequest(customer, stylist, service, at(start), ZONE), existing, customerActive, now);
    }

    private Appointment book(String start) {
        return book(ana, HAIRCUT, start, List.of(), 0, NOW);
    }

    private static void assertViolates(String ruleCode, Executable action) {
        assertThatThrownBy(action::execute)
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo(ruleCode);
    }

    @Nested
    class SuccessfulBooking {

        @Test
        void createsPendingAppointmentWithFrozenPriceAndStylistBranch() {
            Appointment appointment = book("2026-11-09T10:00");

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(appointment.customerId()).isEqualTo(customer);
            assertThat(appointment.stylistId()).isEqualTo(ana.id());
            assertThat(appointment.branchId()).isEqualTo(BRANCH);                     // RN-13
            assertThat(appointment.price()).isEqualTo(Money.usd("12.00"));            // RN-09
            assertThat(appointment.slot()).isEqualTo(TimeSlot.of(at("2026-11-09T10:00"), Duration.ofMinutes(45)));
        }

        @Test
        void allowsBookingRightAfterAnotherAppointment() {
            Appointment previous = appointment(ana, "2026-11-09T09:15", Duration.ofMinutes(45), AppointmentStatus.CONFIRMED);

            Appointment appointment = book(ana, HAIRCUT, "2026-11-09T10:00", List.of(previous), 0, NOW);

            assertThat(appointment.slot().start()).isEqualTo(previous.slot().end());
        }
    }

    @Nested
    class Rules {

        @Test
        void rn01_rejectsOverlapWithAnActiveAppointment() {
            Appointment existing = appointment(ana, "2026-11-09T10:30", Duration.ofMinutes(45), AppointmentStatus.PENDING);

            assertViolates("RN-01", () -> book(ana, HAIRCUT, "2026-11-09T10:00", List.of(existing), 0, NOW));
        }

        @Test
        void rn01_cancelledAppointmentsDoNotBlockTheSlot() {
            Appointment cancelled = appointment(ana, "2026-11-09T10:00", Duration.ofMinutes(45), AppointmentStatus.CANCELLED);

            Appointment appointment = book(ana, HAIRCUT, "2026-11-09T10:00", List.of(cancelled), 0, NOW);

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
        }

        @Test
        void rn01_appointmentsOfOtherStylistsDoNotBlockTheSlot() {
            Appointment otherStylists = appointment(stylist(), "2026-11-09T10:00", Duration.ofMinutes(45),
                    AppointmentStatus.CONFIRMED);

            Appointment appointment = book(ana, HAIRCUT, "2026-11-09T10:00", List.of(otherStylists), 0, NOW);

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
        }

        @Test
        void rn02_rejectsSlotThatRunsIntoTheLunchBreak() {
            assertViolates("RN-02", () -> book("2026-11-09T12:30"));
        }

        @Test
        void rn02_rejectsDayWithoutWorkingHours() {
            assertViolates("RN-02", () -> book("2026-11-10T10:00"));
        }

        @Test
        void rn02_rejectsSlotDuringTimeOff() {
            Stylist onLeave = stylist(new TimeOff(
                    new TimeSlot(at("2026-11-09T09:00"), at("2026-11-09T11:00")), "Cita médica"));

            assertViolates("RN-02", () -> book(onLeave, HAIRCUT, "2026-11-09T10:00", List.of(), 0, NOW));
        }

        @Test
        void rn03_rejectsServiceTheStylistDoesNotPerform() {
            assertViolates("RN-03", () -> book(ana, COLORING, "2026-11-09T10:00", List.of(), 0, NOW));
        }

        @Test
        void rn04_rejectsBookingWithLessThanOneHourNotice() {
            assertViolates("RN-04", () -> book(ana, HAIRCUT, "2026-11-09T10:00", List.of(), 0, at("2026-11-09T09:01")));
        }

        @Test
        void rn04_acceptsBookingWithExactlyOneHourNotice() {
            Appointment appointment = book(ana, HAIRCUT, "2026-11-09T10:00", List.of(), 0, at("2026-11-09T09:00"));

            assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
        }

        @Test
        void rn05_rejectsBookingMoreThan30DaysAhead() {
            Instant monthBefore = at("2026-10-09T08:00");

            assertViolates("RN-05", () -> book(ana, HAIRCUT, "2026-11-09T10:00", List.of(), 0, monthBefore));
        }

        @Test
        void rn06_rejectsStartTimeNotAlignedTo15Minutes() {
            assertViolates("RN-06", () -> book("2026-11-09T10:05"));
        }

        @Test
        void rn08_rejectsCustomerWithMaximumActiveAppointments() {
            assertViolates("RN-08", () -> book(ana, HAIRCUT, "2026-11-09T10:00", List.of(), 3, NOW));
        }

        @Test
        void rejectsInactiveService() {
            SalonService inactive = new SalonService(SalonServiceId.newId(), "Peinado", Duration.ofMinutes(30),
                    Money.usd("10.00"), false);
            Stylist stylist = new Stylist(StylistId.newId(), BRANCH, "Luis", true, Set.of(inactive.id()),
                    MONDAY_SHIFTS, List.of());

            assertViolates("SERVICE_INACTIVE", () -> book(stylist, inactive, "2026-11-09T10:00", List.of(), 0, NOW));
        }
    }

    @Nested
    class Availability {

        @Test
        void offersEvery15MinutesWithinEachShift() {
            List<TimeSlot> slots = booker.availableSlots(ana, HAIRCUT, MONDAY, ZONE, List.of(), at("2026-11-08T08:00"));

            // Mañana: 09:00..12:15 (14 horarios). Tarde: 14:00..17:15 (14 horarios).
            assertThat(slots).hasSize(28);
            assertThat(slots.getFirst().start()).isEqualTo(at("2026-11-09T09:00"));
            assertThat(slots.getLast().end()).isEqualTo(at("2026-11-09T18:00"));
            assertThat(slots).noneMatch(slot -> slot.start().equals(at("2026-11-09T12:30")));
        }

        @Test
        void excludesSlotsOverlappingExistingAppointments() {
            Appointment existing = appointment(ana, "2026-11-09T10:00", Duration.ofMinutes(45), AppointmentStatus.CONFIRMED);

            List<TimeSlot> slots = booker.availableSlots(ana, HAIRCUT, MONDAY, ZONE, List.of(existing), at("2026-11-08T08:00"));

            assertThat(slots).hasSize(28 - 5);
            assertThat(slots).extracting(TimeSlot::start)
                    .contains(at("2026-11-09T09:15"), at("2026-11-09T10:45"))
                    .doesNotContain(at("2026-11-09T09:30"), at("2026-11-09T10:00"), at("2026-11-09T10:30"));
        }

        @Test
        void respectsMinimumAdvanceOnTheSameDay() {
            List<TimeSlot> slots = booker.availableSlots(ana, HAIRCUT, MONDAY, ZONE, List.of(), at("2026-11-09T11:10"));

            assertThat(slots.getFirst().start()).isEqualTo(at("2026-11-09T12:15"));
            assertThat(slots).hasSize(1 + 14);
        }

        @Test
        void returnsNothingOnDaysOff() {
            assertThat(booker.availableSlots(ana, HAIRCUT, MONDAY.plusDays(1), ZONE, List.of(), NOW)).isEmpty();
        }

        @Test
        void everyOfferedSlotCanActuallyBeBooked() {
            Instant now = at("2026-11-08T08:00");

            for (TimeSlot slot : booker.availableSlots(ana, HAIRCUT, MONDAY, ZONE, List.of(), now)) {
                Appointment appointment = booker.book(
                        new BookingRequest(customer, ana, HAIRCUT, slot.start(), ZONE), List.of(), 0, now);
                assertThat(appointment.slot()).isEqualTo(slot);
            }
        }
    }
}

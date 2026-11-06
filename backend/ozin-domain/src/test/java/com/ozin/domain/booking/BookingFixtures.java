package com.ozin.domain.booking;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.Money;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;
import com.ozin.domain.staff.WorkingHours;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Datos de prueba compartidos.
 *
 * <p>Escenario: el lunes 9 de noviembre de 2026, hora de Ecuador. La estilista trabaja los lunes
 * de 09:00 a 13:00 y de 14:00 a 18:00 (pausa de almuerzo) y realiza el servicio "Corte" (45 min, USD 12.00).
 */
final class BookingFixtures {

    static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    static final BranchId BRANCH = BranchId.newId();
    static final SalonService HAIRCUT = new SalonService(
            SalonServiceId.newId(), "Corte", Duration.ofMinutes(45), Money.usd("12.00"), true);
    static final SalonService COLORING = new SalonService(
            SalonServiceId.newId(), "Tinte", Duration.ofMinutes(90), Money.usd("35.00"), true);
    static final List<WorkingHours> MONDAY_SHIFTS = List.of(
            new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
            new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

    private BookingFixtures() {
    }

    /** Convierte una fecha y hora local de la sede, por ejemplo {@code "2026-11-09T10:00"}, a un {@link Instant}. */
    static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
    }

    static Stylist stylist(TimeOff... timeOffs) {
        return new Stylist(StylistId.newId(), BRANCH, "Ana Pérez", true,
                Set.of(HAIRCUT.id()), MONDAY_SHIFTS, List.of(timeOffs));
    }

    static Appointment appointment(Stylist stylist, String start, Duration duration, AppointmentStatus status) {
        return Appointment.restore(AppointmentId.newId(), CustomerId.newId(), stylist.id(), HAIRCUT.id(),
                stylist.branchId(), TimeSlot.of(at(start), duration), Money.usd("12.00"), status, null);
    }

    static Appointment confirmedAppointmentAt(String start) {
        return appointment(stylist(), start, Duration.ofMinutes(45), AppointmentStatus.CONFIRMED);
    }
}

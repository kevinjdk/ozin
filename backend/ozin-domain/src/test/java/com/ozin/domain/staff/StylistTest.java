package com.ozin.domain.staff;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.shared.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StylistTest {

    private static final WorkingHours MORNING =
            new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

    private static Stylist stylist() {
        return new Stylist(StylistId.newId(), BranchId.newId(), "Ana", true, Set.of(), List.of(MORNING), List.of());
    }

    private static TimeOff timeOff(String start, String end) {
        return new TimeOff(new TimeSlot(Instant.parse(start), Instant.parse(end)), "Permiso");
    }

    @Test
    void rejectsOverlappingWorkingHoursOnTheSameDay() {
        WorkingHours overlapping = new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0));

        assertThatThrownBy(() -> new Stylist(StylistId.newId(), BranchId.newId(), "Ana", true, Set.of(),
                List.of(MORNING, overlapping), List.of()))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reviseReplacesServicesAndWorkingHours() {
        Stylist stylist = stylist();
        SalonServiceId service = SalonServiceId.newId();
        WorkingHours tuesday = new WorkingHours(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));

        stylist.revise("Ana María", false, Set.of(service), List.of(tuesday));

        assertThat(stylist.fullName()).isEqualTo("Ana María");
        assertThat(stylist.isActive()).isFalse();
        assertThat(stylist.canPerform(service)).isTrue();
        assertThat(stylist.workingHoursOn(DayOfWeek.MONDAY)).isEmpty();
        assertThat(stylist.workingHoursOn(DayOfWeek.TUESDAY)).containsExactly(tuesday);
    }

    @Test
    void addTimeOffRejectsOverlapWithAnExistingOne() {
        Stylist stylist = stylist();
        stylist.addTimeOff(timeOff("2026-11-09T15:00:00Z", "2026-11-09T17:00:00Z"));

        assertThatThrownBy(() -> stylist.addTimeOff(timeOff("2026-11-09T16:00:00Z", "2026-11-09T18:00:00Z")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(stylist.timeOffs()).hasSize(1);
    }
}

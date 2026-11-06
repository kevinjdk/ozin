package com.ozin.domain.shared;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    private static final Instant TEN = Instant.parse("2026-11-09T15:00:00Z");

    private static TimeSlot slot(int startOffsetMinutes, int durationMinutes) {
        return TimeSlot.of(TEN.plus(Duration.ofMinutes(startOffsetMinutes)), Duration.ofMinutes(durationMinutes));
    }

    @Test
    void rejectsStartEqualOrAfterEnd() {
        assertThatThrownBy(() -> new TimeSlot(TEN, TEN)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> TimeSlot.of(TEN, Duration.ZERO)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void detectsPartialOverlap() {
        assertThat(slot(0, 45).overlapsWith(slot(30, 45))).isTrue();
    }

    @Test
    void detectsContainedSlotAsOverlap() {
        assertThat(slot(0, 90).overlapsWith(slot(15, 15))).isTrue();
    }

    @Test
    void contiguousSlotsDoNotOverlap() {
        assertThat(slot(0, 45).overlapsWith(slot(45, 45))).isFalse();
        assertThat(slot(45, 45).overlapsWith(slot(0, 45))).isFalse();
    }

    @Test
    void containsChecksBothBoundaries() {
        assertThat(slot(0, 60).contains(slot(0, 60))).isTrue();
        assertThat(slot(0, 60).contains(slot(30, 45))).isFalse();
    }
}

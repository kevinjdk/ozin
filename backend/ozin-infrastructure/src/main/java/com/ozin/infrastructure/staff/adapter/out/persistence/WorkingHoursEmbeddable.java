package com.ozin.infrastructure.staff.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
record WorkingHoursEmbeddable(
        @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false) DayOfWeek day,
        @Column(name = "start_time", nullable = false) LocalTime start,
        @Column(name = "end_time", nullable = false) LocalTime end) {
}

package com.ozin.infrastructure.staff.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
record TimeOffEmbeddable(
        @Column(name = "starts_at", nullable = false) Instant start,
        @Column(name = "ends_at", nullable = false) Instant end,
        @Column(nullable = false) String reason) {
}

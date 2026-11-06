package com.ozin.infrastructure.catalog.adapter.out.persistence;

import com.ozin.infrastructure.shared.adapter.out.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "salon_service")
public class SalonServiceJpaEntity extends AuditableEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    @Column(name = "duration_minutes", nullable = false)
    int durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    boolean active;

    protected SalonServiceJpaEntity() {
    }

    SalonServiceJpaEntity(UUID id) {
        this.id = id;
    }
}

package com.ozin.infrastructure.booking.adapter.out.persistence;

import com.ozin.domain.booking.AppointmentStatus;
import com.ozin.domain.booking.CancellationActor;
import com.ozin.infrastructure.shared.adapter.out.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA, distinta de la entidad de dominio {@code Appointment}.
 * Los demás agregados se referencian por identificador, no con asociaciones JPA.
 */
@Entity
@Table(name = "appointment")
class AppointmentJpaEntity extends AuditableEntity {

    @Id
    UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    UUID customerId;

    @Column(name = "stylist_id", nullable = false, updatable = false)
    UUID stylistId;

    @Column(name = "service_id", nullable = false, updatable = false)
    UUID serviceId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    UUID branchId;

    @Column(name = "starts_at", nullable = false, updatable = false)
    Instant startsAt;

    @Column(name = "ends_at", nullable = false, updatable = false)
    Instant endsAt;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(nullable = false, updatable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 16)
    CancellationActor cancelledBy;

    protected AppointmentJpaEntity() {
    }

    AppointmentJpaEntity(UUID id, UUID customerId, UUID stylistId, UUID serviceId, UUID branchId,
                         Instant startsAt, Instant endsAt, BigDecimal price, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.stylistId = stylistId;
        this.serviceId = serviceId;
        this.branchId = branchId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.price = price;
        this.currency = currency;
    }
}

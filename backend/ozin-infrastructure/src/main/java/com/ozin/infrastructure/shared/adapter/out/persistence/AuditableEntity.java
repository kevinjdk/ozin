package com.ozin.infrastructure.shared.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Campos técnicos comunes: auditoría temporal y bloqueo optimista (ADR-0003).
 *
 * <p>{@code version} es {@code null} en una entidad nueva: así Spring Data distingue un alta
 * de una modificación aunque el identificador lo asigne el dominio.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

package com.ozin.infrastructure.identity.adapter.out.persistence;

import com.ozin.domain.identity.Role;
import com.ozin.infrastructure.shared.adapter.out.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserJpaEntity extends AuditableEntity {

    @Id
    UUID id;

    @Column(nullable = false, updatable = false)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    Role role;

    @Column(name = "customer_id", updatable = false)
    UUID customerId;

    @Column(name = "stylist_id", updatable = false)
    UUID stylistId;

    @Column(nullable = false)
    boolean enabled;

    protected UserJpaEntity() {
    }

    UserJpaEntity(UUID id, String email, Role role, UUID customerId, UUID stylistId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.customerId = customerId;
        this.stylistId = stylistId;
    }
}

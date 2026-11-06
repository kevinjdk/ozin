package com.ozin.infrastructure.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    UUID familyId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    Instant expiresAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    protected RefreshTokenJpaEntity() {
    }

    RefreshTokenJpaEntity(UUID id, UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}

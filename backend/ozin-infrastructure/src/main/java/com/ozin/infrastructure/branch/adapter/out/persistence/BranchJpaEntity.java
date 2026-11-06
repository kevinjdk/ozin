package com.ozin.infrastructure.branch.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Solo lectura en el MVP: las sedes se crean por migración (ADR-0006). */
@Entity
@Table(name = "branch")
class BranchJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String address;

    @Column(name = "time_zone", nullable = false)
    String timeZone;

    @Column(nullable = false)
    boolean active;

    protected BranchJpaEntity() {
    }
}

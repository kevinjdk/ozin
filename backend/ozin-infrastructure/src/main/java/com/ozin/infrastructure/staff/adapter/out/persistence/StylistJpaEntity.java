package com.ozin.infrastructure.staff.adapter.out.persistence;

import com.ozin.infrastructure.shared.adapter.out.persistence.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stylist")
public class StylistJpaEntity extends AuditableEntity {

    @Id
    UUID id;

    @Column(name = "branch_id", nullable = false, updatable = false)
    UUID branchId;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(nullable = false)
    boolean active;

    @ElementCollection
    @CollectionTable(name = "stylist_service", joinColumns = @JoinColumn(name = "stylist_id"))
    @Column(name = "service_id", nullable = false)
    Set<UUID> serviceIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "working_hours", joinColumns = @JoinColumn(name = "stylist_id"))
    Set<WorkingHoursEmbeddable> workingHours = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "time_off", joinColumns = @JoinColumn(name = "stylist_id"))
    Set<TimeOffEmbeddable> timeOffs = new HashSet<>();

    protected StylistJpaEntity() {
    }

    StylistJpaEntity(UUID id, UUID branchId) {
        this.id = id;
        this.branchId = branchId;
    }
}

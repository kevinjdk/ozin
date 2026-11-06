package com.ozin.infrastructure.customer.adapter.out.persistence;

import com.ozin.infrastructure.shared.adapter.out.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "customer")
public class CustomerJpaEntity extends AuditableEntity {

    @Id
    UUID id;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String phone;

    protected CustomerJpaEntity() {
    }

    CustomerJpaEntity(UUID id, String fullName, String email, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }
}

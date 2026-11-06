package com.ozin.infrastructure.staff.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface StylistJpaRepository extends JpaRepository<StylistJpaEntity, UUID> {

    List<StylistJpaEntity> findAllByOrderByFullName();

    List<StylistJpaEntity> findByActiveTrueOrderByFullName();
}

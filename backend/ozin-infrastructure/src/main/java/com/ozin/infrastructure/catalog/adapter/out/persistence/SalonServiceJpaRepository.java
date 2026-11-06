package com.ozin.infrastructure.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SalonServiceJpaRepository extends JpaRepository<SalonServiceJpaEntity, UUID> {

    List<SalonServiceJpaEntity> findAllByOrderByName();

    List<SalonServiceJpaEntity> findByActiveTrueOrderByName();

    long countByIdIn(Collection<UUID> ids);
}

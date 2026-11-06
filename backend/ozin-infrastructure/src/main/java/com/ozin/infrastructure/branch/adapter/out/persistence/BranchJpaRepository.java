package com.ozin.infrastructure.branch.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, UUID> {

    List<BranchJpaEntity> findByActiveTrueOrderByName();
}

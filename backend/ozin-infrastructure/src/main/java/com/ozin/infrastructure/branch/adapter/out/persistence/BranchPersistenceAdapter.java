package com.ozin.infrastructure.branch.adapter.out.persistence;

import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.domain.branch.Branch;
import com.ozin.domain.branch.BranchId;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
class BranchPersistenceAdapter implements LoadBranchPort {

    private final BranchJpaRepository repository;

    BranchPersistenceAdapter(BranchJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Branch> findById(BranchId id) {
        return repository.findById(id.value()).map(BranchPersistenceAdapter::toDomain);
    }

    @Override
    public List<Branch> findAllActive() {
        return repository.findByActiveTrueOrderByName().stream().map(BranchPersistenceAdapter::toDomain).toList();
    }

    private static Branch toDomain(BranchJpaEntity entity) {
        return new Branch(BranchId.of(entity.id), entity.name, entity.address,
                ZoneId.of(entity.timeZone), entity.active);
    }
}

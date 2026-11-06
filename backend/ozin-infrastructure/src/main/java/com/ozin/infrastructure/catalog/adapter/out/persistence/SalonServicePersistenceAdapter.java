package com.ozin.infrastructure.catalog.adapter.out.persistence;

import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.catalog.port.out.SaveSalonServicePort;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.Money;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Component
class SalonServicePersistenceAdapter implements LoadSalonServicePort, SaveSalonServicePort {

    private final SalonServiceJpaRepository repository;

    SalonServicePersistenceAdapter(SalonServiceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SalonService> findById(SalonServiceId id) {
        return repository.findById(id.value()).map(SalonServicePersistenceAdapter::toDomain);
    }

    @Override
    public List<SalonService> findAll(boolean includeInactive) {
        List<SalonServiceJpaEntity> entities = includeInactive
                ? repository.findAllByOrderByName()
                : repository.findByActiveTrueOrderByName();
        return entities.stream().map(SalonServicePersistenceAdapter::toDomain).toList();
    }

    @Override
    public long countExisting(Collection<SalonServiceId> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return repository.countByIdIn(ids.stream().map(SalonServiceId::value).toList());
    }

    @Override
    public void save(SalonService service) {
        SalonServiceJpaEntity entity = repository.findById(service.id().value())
                .orElseGet(() -> new SalonServiceJpaEntity(service.id().value()));
        entity.name = service.name();
        entity.durationMinutes = Math.toIntExact(service.duration().toMinutes());
        entity.price = service.price().amount();
        entity.currency = service.price().currency().getCurrencyCode();
        entity.active = service.isActive();
        repository.saveAndFlush(entity);
    }

    private static SalonService toDomain(SalonServiceJpaEntity entity) {
        return new SalonService(SalonServiceId.of(entity.id), entity.name, Duration.ofMinutes(entity.durationMinutes),
                new Money(entity.price, Currency.getInstance(entity.currency)), entity.active);
    }
}

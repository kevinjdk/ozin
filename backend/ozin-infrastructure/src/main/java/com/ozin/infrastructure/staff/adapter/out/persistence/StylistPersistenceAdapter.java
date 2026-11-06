package com.ozin.infrastructure.staff.adapter.out.persistence;

import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;
import com.ozin.domain.staff.WorkingHours;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las lecturas son transaccionales porque las colecciones se cargan de forma diferida y la conversión
 * a dominio ocurre aquí. Si ya hay una transacción abierta por el caso de uso, se unen a ella.
 */
@Component
class StylistPersistenceAdapter implements LoadStylistPort, SaveStylistPort {

    private final StylistJpaRepository repository;

    StylistPersistenceAdapter(StylistJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stylist> findById(StylistId id) {
        return repository.findById(id.value()).map(StylistPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stylist> findAll(boolean includeInactive) {
        List<StylistJpaEntity> entities = includeInactive
                ? repository.findAllByOrderByFullName()
                : repository.findByActiveTrueOrderByFullName();
        return entities.stream().map(StylistPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Stylist stylist) {
        StylistJpaEntity entity = repository.findById(stylist.id().value())
                .orElseGet(() -> new StylistJpaEntity(stylist.id().value(), stylist.branchId().value()));
        entity.fullName = stylist.fullName();
        entity.active = stylist.isActive();
        replace(entity.serviceIds, stylist.services(), SalonServiceId::value);
        replace(entity.workingHours, stylist.workingHours(),
                wh -> new WorkingHoursEmbeddable(wh.day(), wh.start(), wh.end()));
        replace(entity.timeOffs, stylist.timeOffs(),
                off -> new TimeOffEmbeddable(off.period().start(), off.period().end(), off.reason()));
        repository.saveAndFlush(entity);
    }

    /** Modifica la colección existente para que Hibernate solo escriba las diferencias. */
    private static <D, E> void replace(Set<E> target, Collection<D> source, Function<D, E> mapper) {
        Set<E> updated = source.stream().map(mapper).collect(Collectors.toSet());
        target.retainAll(updated);
        target.addAll(updated);
    }

    private static Stylist toDomain(StylistJpaEntity entity) {
        return new Stylist(StylistId.of(entity.id), BranchId.of(entity.branchId), entity.fullName, entity.active,
                entity.serviceIds.stream().map(SalonServiceId::of).toList(),
                entity.workingHours.stream()
                        .map(wh -> new WorkingHours(wh.day(), wh.start(), wh.end()))
                        .toList(),
                entity.timeOffs.stream()
                        .map(off -> new TimeOff(new TimeSlot(off.start(), off.end()), off.reason()))
                        .toList());
    }
}

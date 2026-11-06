package com.ozin.infrastructure.booking.adapter.out.persistence;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.LoadAppointmentViewsPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.shared.PageQuery;
import com.ozin.application.shared.PageResult;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentId;
import com.ozin.domain.booking.AppointmentStatus;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.Money;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.StylistId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Component
class AppointmentPersistenceAdapter implements LoadAppointmentPort, SaveAppointmentPort, LoadAppointmentViewsPort {

    private static final String NO_OVERLAP_CONSTRAINT = "appointment_no_overlap";

    /** Qué estados cuentan como activos lo decide el dominio. */
    private static final List<AppointmentStatus> ACTIVE_STATUSES = Arrays.stream(AppointmentStatus.values())
            .filter(AppointmentStatus::isActive)
            .toList();

    private final AppointmentJpaRepository repository;

    AppointmentPersistenceAdapter(AppointmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id) {
        return repository.findById(id.value()).map(AppointmentPersistenceAdapter::toDomain);
    }

    @Override
    public List<Appointment> findActiveByStylistOverlapping(StylistId stylistId, TimeSlot period) {
        return repository.findByStylistOverlapping(stylistId.value(), ACTIVE_STATUSES, period.start(), period.end())
                .stream()
                .map(AppointmentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public long countUpcomingActiveByCustomer(CustomerId customerId, Instant now) {
        return repository.countByCustomerIdAndStatusInAndStartsAtAfter(customerId.value(), ACTIVE_STATUSES, now);
    }

    @Override
    public void save(Appointment appointment) {
        AppointmentJpaEntity entity = repository.findById(appointment.id().value())
                .orElseGet(() -> newEntity(appointment));
        entity.status = appointment.status();
        entity.cancelledBy = appointment.cancelledBy().orElse(null);
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(NO_OVERLAP_CONSTRAINT)) {
                throw new BusinessRuleViolationException("RN-01", "El horario seleccionado ya no está disponible");
            }
            throw ex;
        }
    }

    @Override
    public PageResult<AppointmentView> findByCustomer(CustomerId customerId, PageQuery page) {
        Page<AppointmentView> result = repository.findViewsByCustomer(
                customerId.value(), PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent(), result.getTotalElements(), page.page(), page.size());
    }

    @Override
    public List<AppointmentView> findByStylist(StylistId stylistId, AgendaRange range) {
        return repository.findViewsByStylist(stylistId.value(), range.from(), range.to());
    }

    @Override
    public List<AppointmentView> findByBranch(BranchId branchId, AgendaRange range) {
        return repository.findViewsByBranch(branchId.value(), range.from(), range.to());
    }

    private static AppointmentJpaEntity newEntity(Appointment appointment) {
        return new AppointmentJpaEntity(appointment.id().value(), appointment.customerId().value(),
                appointment.stylistId().value(), appointment.serviceId().value(), appointment.branchId().value(),
                appointment.slot().start(), appointment.slot().end(), appointment.price().amount(),
                appointment.price().currency().getCurrencyCode());
    }

    private static Appointment toDomain(AppointmentJpaEntity entity) {
        return Appointment.restore(AppointmentId.of(entity.id), CustomerId.of(entity.customerId),
                StylistId.of(entity.stylistId), SalonServiceId.of(entity.serviceId), BranchId.of(entity.branchId),
                new TimeSlot(entity.startsAt, entity.endsAt),
                new Money(entity.price, Currency.getInstance(entity.currency)),
                entity.status, entity.cancelledBy);
    }
}

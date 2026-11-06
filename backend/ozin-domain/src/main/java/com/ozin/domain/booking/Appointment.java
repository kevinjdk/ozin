package com.ozin.domain.booking;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.InvalidStateTransitionException;
import com.ozin.domain.shared.Money;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.StylistId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Cita reservada por un cliente. Raíz de agregado del módulo de reservas.
 *
 * <p>Modelo rico: el estado solo cambia mediante métodos que aplican las reglas
 * ({@link #confirm()}, {@link #cancel}, {@link #complete}, {@link #markNoShow}); no existen setters.
 *
 * <p>Las citas nuevas solo se crean a través de {@link AppointmentBooker}, que valida todas las
 * reglas de reserva; por eso {@link #book} es de visibilidad de paquete.
 */
public final class Appointment {

    private final AppointmentId id;
    private final CustomerId customerId;
    private final StylistId stylistId;
    private final SalonServiceId serviceId;
    private final BranchId branchId;
    private final TimeSlot slot;
    private final Money price;
    private AppointmentStatus status;
    private CancellationActor cancelledBy;

    private Appointment(AppointmentId id, CustomerId customerId, StylistId stylistId, SalonServiceId serviceId,
                        BranchId branchId, TimeSlot slot, Money price, AppointmentStatus status,
                        CancellationActor cancelledBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.stylistId = Objects.requireNonNull(stylistId, "stylistId");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.price = Objects.requireNonNull(price, "price");
        this.status = Objects.requireNonNull(status, "status");
        this.cancelledBy = cancelledBy;
    }

    /** Crea una cita nueva en estado {@code PENDING}. El precio queda congelado (RN-09). */
    static Appointment book(CustomerId customerId, StylistId stylistId, SalonServiceId serviceId,
                            BranchId branchId, TimeSlot slot, Money price) {
        return new Appointment(AppointmentId.newId(), customerId, stylistId, serviceId, branchId,
                slot, price, AppointmentStatus.PENDING, null);
    }

    /** Reconstruye una cita existente (por ejemplo, desde la base de datos). No aplica reglas de reserva. */
    public static Appointment restore(AppointmentId id, CustomerId customerId, StylistId stylistId,
                                      SalonServiceId serviceId, BranchId branchId, TimeSlot slot, Money price,
                                      AppointmentStatus status, CancellationActor cancelledBy) {
        return new Appointment(id, customerId, stylistId, serviceId, branchId, slot, price, status, cancelledBy);
    }

    public void confirm() {
        requireStatus(AppointmentStatus.PENDING, "confirmar");
        status = AppointmentStatus.CONFIRMED;
    }

    public void cancel(CancellationActor actor, Instant now, CancellationPolicy policy) {
        Objects.requireNonNull(actor, "actor");
        if (!status.isActive()) {
            throw new InvalidStateTransitionException(status, "cancelar");
        }
        policy.ensureCanCancel(this, actor, now);
        status = AppointmentStatus.CANCELLED;
        cancelledBy = actor;
    }

    public void complete(Instant now) {
        requireStatus(AppointmentStatus.CONFIRMED, "completar");
        requireStarted(now, "completar");
        status = AppointmentStatus.COMPLETED;
    }

    public void markNoShow(Instant now) {
        requireStatus(AppointmentStatus.CONFIRMED, "marcar como no asistida");
        requireStarted(now, "marcar como no asistida");
        status = AppointmentStatus.NO_SHOW;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean overlapsWith(TimeSlot other) {
        return slot.overlapsWith(other);
    }

    private void requireStatus(AppointmentStatus expected, String action) {
        if (status != expected) {
            throw new InvalidStateTransitionException(status, action);
        }
    }

    private void requireStarted(Instant now, String action) {
        if (now.isBefore(slot.start())) {
            throw new InvalidStateTransitionException(status, action + " antes de su hora de inicio");
        }
    }

    public AppointmentId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public StylistId stylistId() {
        return stylistId;
    }

    public SalonServiceId serviceId() {
        return serviceId;
    }

    public BranchId branchId() {
        return branchId;
    }

    public TimeSlot slot() {
        return slot;
    }

    public Money price() {
        return price;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Optional<CancellationActor> cancelledBy() {
        return Optional.ofNullable(cancelledBy);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Appointment other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

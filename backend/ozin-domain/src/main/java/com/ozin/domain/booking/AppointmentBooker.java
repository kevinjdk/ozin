package com.ozin.domain.booking;

import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.TimeSlot;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.WorkingHours;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de dominio que aplica las reglas de reserva.
 *
 * <p>Concentra en un único lugar la validación de un horario, de modo que
 * {@link #book} (reservar) y {@link #availableSlots} (consultar disponibilidad) nunca puedan
 * discrepar: un horario ofrecido como disponible siempre es reservable, y viceversa.
 *
 * <p>Recibe {@code now} como parámetro en lugar de leer el reloj del sistema, para que el
 * comportamiento sea determinista y fácil de probar.
 */
public final class AppointmentBooker {

    private final BookingPolicy policy;

    public AppointmentBooker(BookingPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Crea una cita si cumple todas las reglas de reserva.
     *
     * @param stylistAppointments         citas existentes del estilista en torno al horario solicitado
     * @param customerActiveAppointments  número de citas activas futuras del cliente
     * @throws BusinessRuleViolationException si se incumple alguna regla
     */
    public Appointment book(BookingRequest request,
                            Collection<Appointment> stylistAppointments,
                            long customerActiveAppointments,
                            Instant now) {
        if (customerActiveAppointments >= policy.maxActiveAppointmentsPerCustomer()) {
            throw new BusinessRuleViolationException("RN-08",
                    "Ya tienes %d citas activas, que es el máximo permitido"
                            .formatted(policy.maxActiveAppointmentsPerCustomer()));
        }

        Stylist stylist = request.stylist();
        SalonService service = request.service();
        TimeSlot slot = TimeSlot.of(request.start(), service.duration());

        checkSlot(stylist, service, slot, request.branchZone(), stylistAppointments, now)
                .ifPresent(violation -> {
                    throw violation.toException();
                });

        return Appointment.book(request.customerId(), stylist.id(), service.id(), stylist.branchId(),
                slot, service.price());
    }

    /**
     * Calcula los horarios reservables de un estilista para un servicio en una fecha (HU-02).
     *
     * @param date fecha local de la sede
     */
    public List<TimeSlot> availableSlots(Stylist stylist,
                                         SalonService service,
                                         LocalDate date,
                                         ZoneId branchZone,
                                         Collection<Appointment> stylistAppointments,
                                         Instant now) {
        Duration duration = service.duration();
        List<TimeSlot> available = new ArrayList<>();

        for (WorkingHours hours : stylist.workingHoursOn(date.getDayOfWeek())) {
            ZonedDateTime cursor = date.atTime(alignUp(hours.start())).atZone(branchZone);
            ZonedDateTime limit = date.atTime(hours.end()).atZone(branchZone);

            while (!cursor.plus(duration).isAfter(limit)) {
                TimeSlot candidate = TimeSlot.of(cursor.toInstant(), duration);
                if (checkSlot(stylist, service, candidate, branchZone, stylistAppointments, now).isEmpty()) {
                    available.add(candidate);
                }
                cursor = cursor.plus(policy.slotGranularity());
            }
        }
        return List.copyOf(available);
    }

    private Optional<RuleViolation> checkSlot(Stylist stylist,
                                              SalonService service,
                                              TimeSlot slot,
                                              ZoneId zone,
                                              Collection<Appointment> stylistAppointments,
                                              Instant now) {
        if (!service.isActive()) {
            return RuleViolation.of("SERVICE_INACTIVE", "El servicio no está disponible");
        }
        if (!stylist.isActive()) {
            return RuleViolation.of("STYLIST_INACTIVE", "El estilista no está disponible");
        }
        if (!stylist.canPerform(service.id())) {
            return RuleViolation.of("RN-03", "El estilista no realiza el servicio seleccionado");
        }
        if (!isAligned(slot.start(), zone)) {
            return RuleViolation.of("RN-06", "El horario debe comenzar en intervalos de %d minutos"
                    .formatted(policy.slotGranularity().toMinutes()));
        }
        if (slot.start().isBefore(now.plus(policy.minimumAdvance()))) {
            return RuleViolation.of("RN-04", "Debes reservar con al menos %d minutos de anticipación"
                    .formatted(policy.minimumAdvance().toMinutes()));
        }
        if (slot.start().isAfter(now.plus(policy.maximumAdvance()))) {
            return RuleViolation.of("RN-05", "Solo puedes reservar con hasta %d días de anticipación"
                    .formatted(policy.maximumAdvance().toDays()));
        }
        if (!stylist.isAvailableFor(slot, zone)) {
            return RuleViolation.of("RN-02", "El estilista no atiende en ese horario");
        }
        boolean overlaps = stylistAppointments.stream()
                .filter(appointment -> appointment.stylistId().equals(stylist.id()))
                .filter(Appointment::isActive)
                .anyMatch(appointment -> appointment.overlapsWith(slot));
        if (overlaps) {
            return RuleViolation.of("RN-01", "El horario seleccionado ya no está disponible");
        }
        return Optional.empty();
    }

    private boolean isAligned(Instant start, ZoneId zone) {
        LocalTime local = start.atZone(zone).toLocalTime();
        return local.getNano() == 0 && local.toSecondOfDay() % policy.slotGranularity().toSeconds() == 0;
    }

    private LocalTime alignUp(LocalTime time) {
        long step = policy.slotGranularity().toSeconds();
        long seconds = time.toSecondOfDay() + (time.getNano() > 0 ? 1 : 0);
        long aligned = ((seconds + step - 1) / step) * step;
        return aligned >= Duration.ofDays(1).toSeconds() ? LocalTime.MAX : LocalTime.ofSecondOfDay(aligned);
    }

    private record RuleViolation(String code, String message) {

        static Optional<RuleViolation> of(String code, String message) {
            return Optional.of(new RuleViolation(code, message));
        }

        BusinessRuleViolationException toException() {
            return new BusinessRuleViolationException(code, message);
        }
    }
}

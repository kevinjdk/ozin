package com.ozin.domain.staff;

import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.shared.DomainValidationException;
import com.ozin.domain.shared.TimeSlot;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Estilista de una sede, con los servicios que realiza, su horario semanal y sus ausencias. */
public final class Stylist {

    private final StylistId id;
    private final BranchId branchId;
    private String fullName;
    private boolean active;
    private Set<SalonServiceId> services;
    private List<WorkingHours> workingHours;
    private List<TimeOff> timeOffs;

    public Stylist(StylistId id,
                   BranchId branchId,
                   String fullName,
                   boolean active,
                   Collection<SalonServiceId> services,
                   Collection<WorkingHours> workingHours,
                   Collection<TimeOff> timeOffs) {
        this.id = Objects.requireNonNull(id, "id");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.fullName = requireName(fullName);
        this.active = active;
        this.services = Set.copyOf(services);
        this.workingHours = requireNoOverlaps(workingHours);
        this.timeOffs = List.copyOf(timeOffs);
    }

    /**
     * Actualiza los datos del estilista. No modifica las citas ya reservadas: el administrador
     * las revisa en la agenda si el nuevo horario las deja fuera de la jornada.
     */
    public void revise(String fullName,
                       boolean active,
                       Collection<SalonServiceId> services,
                       Collection<WorkingHours> workingHours) {
        this.fullName = requireName(fullName);
        this.active = active;
        this.services = Set.copyOf(services);
        this.workingHours = requireNoOverlaps(workingHours);
    }

    public void addTimeOff(TimeOff timeOff) {
        Objects.requireNonNull(timeOff, "timeOff");
        if (isOffDuring(timeOff.period())) {
            throw new DomainValidationException("La ausencia se solapa con otra ya registrada");
        }
        List<TimeOff> updated = new ArrayList<>(timeOffs);
        updated.add(timeOff);
        this.timeOffs = List.copyOf(updated);
    }

    /** RN-03: el estilista solo atiende los servicios para los que está habilitado. */
    public boolean canPerform(SalonServiceId serviceId) {
        return services.contains(serviceId);
    }

    public List<WorkingHours> workingHoursOn(DayOfWeek day) {
        return workingHours.stream()
                .filter(wh -> wh.day() == day)
                .sorted(Comparator.comparing(WorkingHours::start))
                .toList();
    }

    /** RN-02: el intervalo cabe completo en una franja laboral y no coincide con ninguna ausencia. */
    public boolean isAvailableFor(TimeSlot slot, ZoneId zone) {
        return isWorkingDuring(slot, zone) && !isOffDuring(slot);
    }

    private boolean isWorkingDuring(TimeSlot slot, ZoneId zone) {
        ZonedDateTime start = slot.start().atZone(zone);
        ZonedDateTime end = slot.end().atZone(zone);
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        return workingHoursOn(start.getDayOfWeek()).stream()
                .anyMatch(wh -> wh.covers(start.toLocalTime(), end.toLocalTime()));
    }

    private boolean isOffDuring(TimeSlot slot) {
        return timeOffs.stream().anyMatch(off -> off.period().overlapsWith(slot));
    }

    private static String requireName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainValidationException("El nombre del estilista es obligatorio");
        }
        return fullName.strip();
    }

    private static List<WorkingHours> requireNoOverlaps(Collection<WorkingHours> workingHours) {
        List<WorkingHours> copy = List.copyOf(workingHours);
        for (int i = 0; i < copy.size(); i++) {
            for (int j = i + 1; j < copy.size(); j++) {
                if (copy.get(i).overlapsWith(copy.get(j))) {
                    throw new DomainValidationException("Las franjas laborales de un mismo día no pueden solaparse");
                }
            }
        }
        return copy;
    }

    public StylistId id() {
        return id;
    }

    public BranchId branchId() {
        return branchId;
    }

    public String fullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }

    public Set<SalonServiceId> services() {
        return services;
    }

    public List<WorkingHours> workingHours() {
        return workingHours;
    }

    public List<TimeOff> timeOffs() {
        return timeOffs;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stylist other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

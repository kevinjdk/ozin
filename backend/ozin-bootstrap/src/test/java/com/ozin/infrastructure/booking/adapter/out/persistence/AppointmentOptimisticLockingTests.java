package com.ozin.infrastructure.booking.adapter.out.persistence;

import com.ozin.ApiTestSupport;
import com.ozin.domain.booking.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dos operaciones que parten de la misma versión de una cita no pueden guardarse las dos (ADR-0003). */
class AppointmentOptimisticLockingTests extends ApiTestSupport {

    @Autowired
    private AppointmentJpaRepository repository;

    @Test
    void rejectsSavingAStaleCopyOfAnAppointment() throws Exception {
        String haircut = createService("Corte", 45, "12.00");
        String ana = createStylist(haircut).id();
        UUID id = UUID.fromString(createdId(book(createCustomer(), ana, haircut, "2026-11-09T10:00")));

        AppointmentJpaEntity first = repository.findById(id).orElseThrow();
        AppointmentJpaEntity stale = repository.findById(id).orElseThrow();

        first.status = AppointmentStatus.CONFIRMED;
        repository.saveAndFlush(first);

        stale.status = AppointmentStatus.CANCELLED;
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(repository.findById(id).orElseThrow().status).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}

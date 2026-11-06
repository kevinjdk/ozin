package com.ozin;

import com.ozin.application.booking.port.out.SaveAppointmentPort;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas deben cumplirse también cuando llegan varias peticiones exactamente a la vez. */
class BookingConcurrencyTests extends ApiTestSupport {

    private static final String MONDAY = "2026-11-09";

    @Autowired
    private SaveAppointmentPort saveAppointments;

    private String haircut;
    private TestStylist ana;

    @BeforeEach
    void givenASalon() throws Exception {
        haircut = createService("Corte", 45, "12.00");
        ana = createStylist(haircut);
    }

    private static List<Integer> runSimultaneously(List<Callable<Integer>> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> request : requests) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        }
    }

    private Callable<Integer> bookingOf(TestCustomer customer, String localStart) {
        return () -> book(customer, ana.id(), haircut, localStart).andReturn().getResponse().getStatus();
    }

    private Integer activeAppointments() {
        return jdbc.queryForObject("SELECT count(*) FROM appointment WHERE status = 'PENDING'", Integer.class);
    }

    @Test
    void rn01_onlyOneOfManySimultaneousBookingsGetsTheSlot() throws Exception {
        List<Callable<Integer>> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            requests.add(bookingOf(createCustomer(), MONDAY + "T10:00"));
        }

        List<Integer> statuses = runSimultaneously(requests);

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(activeAppointments()).isEqualTo(1);
    }

    @Test
    void rn08_simultaneousBookingsOfACustomerCannotExceedTheLimit() throws Exception {
        TestCustomer customer = createCustomer();
        List<Callable<Integer>> requests = List.of(
                bookingOf(customer, MONDAY + "T09:00"),
                bookingOf(customer, MONDAY + "T10:00"),
                bookingOf(customer, MONDAY + "T11:00"),
                bookingOf(customer, MONDAY + "T12:00"),
                bookingOf(customer, MONDAY + "T14:00"),
                bookingOf(customer, MONDAY + "T15:00"));

        List<Integer> statuses = runSimultaneously(requests);

        assertThat(statuses).containsOnly(201, 422);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(3);
        assertThat(activeAppointments()).isEqualTo(3);
    }

    @Test
    void rn01_theDatabaseRejectsAnOverlapThatSkippedTheDomainValidation() throws Exception {
        createdId(book(createCustomer(), ana.id(), haircut, MONDAY + "T10:00"));

        // restore() simula una transacción simultánea que validó antes de que existiera la primera cita.
        Appointment overlapping = Appointment.restore(AppointmentId.newId(),
                CustomerId.of(UUID.fromString(createCustomer().id())), StylistId.of(UUID.fromString(ana.id())),
                SalonServiceId.of(UUID.fromString(haircut)),
                BranchId.of(UUID.fromString(DEFAULT_BRANCH)),
                TimeSlot.of(at(MONDAY + "T10:30"), Duration.ofMinutes(45)),
                Money.usd("12.00"), AppointmentStatus.PENDING, null);

        assertThatThrownBy(() -> saveAppointments.save(overlapping))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("RN-01");
        assertThat(activeAppointments()).isEqualTo(1);
    }
}

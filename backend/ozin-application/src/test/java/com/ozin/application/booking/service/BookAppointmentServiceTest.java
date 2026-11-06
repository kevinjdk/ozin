package com.ozin.application.booking.service;

import com.ozin.application.booking.port.in.BookAppointmentUseCase;
import com.ozin.application.booking.port.out.AppointmentNotificationPort;
import com.ozin.application.booking.port.out.LoadAppointmentPort;
import com.ozin.application.booking.port.out.SaveAppointmentPort;
import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.application.catalog.port.out.LoadSalonServicePort;
import com.ozin.application.customer.port.out.LoadCustomerPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.domain.booking.Appointment;
import com.ozin.domain.booking.AppointmentBooker;
import com.ozin.domain.booking.AppointmentStatus;
import com.ozin.domain.booking.BookingPolicy;
import com.ozin.domain.branch.Branch;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.catalog.SalonService;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.shared.Money;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.WorkingHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookAppointmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    private static final Instant NOW = Instant.parse("2026-11-09T13:00:00Z");      // 08:00 en Ecuador
    private static final Instant START = Instant.parse("2026-11-09T15:00:00Z");    // 10:00 en Ecuador

    @Mock private LoadCustomerPort customers;
    @Mock private LoadStylistPort stylists;
    @Mock private LoadSalonServicePort services;
    @Mock private LoadBranchPort branches;
    @Mock private LoadAppointmentPort loadAppointments;
    @Mock private SaveAppointmentPort saveAppointments;
    @Mock private AppointmentNotificationPort notifications;
    @Mock private UnitOfWork unitOfWork;

    private final BranchId branchId = BranchId.newId();
    private final SalonService haircut = new SalonService(
            SalonServiceId.newId(), "Corte", Duration.ofMinutes(45), Money.usd("12.00"), true);
    private final Stylist stylist = new Stylist(StylistId.newId(), branchId, "Ana", true, Set.of(haircut.id()),
            List.of(new WorkingHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))), List.of());
    private final CustomerId customerId = CustomerId.newId();

    private BookAppointmentService service;

    @BeforeEach
    void setUp() {
        lenient().when(unitOfWork.inTransaction(any()))
                .thenAnswer(call -> call.<Supplier<?>>getArgument(0).get());
        service = new BookAppointmentService(customers, stylists, services, branches, loadAppointments,
                saveAppointments, notifications, new AppointmentBooker(BookingPolicy.standard()), unitOfWork,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private BookAppointmentUseCase.Command command() {
        return new BookAppointmentUseCase.Command(customerId, stylist.id(), haircut.id(), START);
    }

    private void givenExistingData() {
        when(customers.lockForBooking(customerId)).thenReturn(true);
        when(stylists.findById(stylist.id())).thenReturn(Optional.of(stylist));
        when(services.findById(haircut.id())).thenReturn(Optional.of(haircut));
        when(branches.findById(branchId)).thenReturn(
                Optional.of(new Branch(branchId, "Matriz", "Quito", ZONE, true)));
    }

    @Test
    void savesAndNotifiesInThatOrder() {
        givenExistingData();

        Appointment appointment = service.book(command());

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.PENDING);
        InOrder inOrder = Mockito.inOrder(saveAppointments, notifications);
        inOrder.verify(saveAppointments).save(appointment);
        inOrder.verify(notifications).appointmentBooked(appointment);
    }

    @Test
    void locksTheCustomerBeforeCountingTheirAppointments() {
        givenExistingData();

        service.book(command());

        InOrder inOrder = Mockito.inOrder(customers, loadAppointments);
        inOrder.verify(customers).lockForBooking(customerId);
        inOrder.verify(loadAppointments).countUpcomingActiveByCustomer(customerId, NOW);
    }

    @Test
    void doesNotNotifyWhenSavingFails() {
        givenExistingData();
        Mockito.doThrow(new BusinessRuleViolationException("RN-01", "Ocupado"))
                .when(saveAppointments).save(any());

        assertThatThrownBy(() -> service.book(command()))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(notifications, never()).appointmentBooked(any());
    }

    @Test
    void doesNotSaveWhenARuleIsViolated() {
        givenExistingData();
        when(loadAppointments.countUpcomingActiveByCustomer(customerId, NOW)).thenReturn(3L);

        assertThatThrownBy(() -> service.book(command()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("RN-08");

        verify(saveAppointments, never()).save(any());
        verify(notifications, never()).appointmentBooked(any());
    }

    @Test
    void failsWhenTheCustomerDoesNotExist() {
        when(customers.lockForBooking(customerId)).thenReturn(false);

        assertThatThrownBy(() -> service.book(command()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

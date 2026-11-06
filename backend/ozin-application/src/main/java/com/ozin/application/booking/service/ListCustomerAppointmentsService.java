package com.ozin.application.booking.service;

import com.ozin.application.booking.AppointmentView;
import com.ozin.application.booking.port.in.ListCustomerAppointmentsUseCase;
import com.ozin.application.booking.port.out.LoadAppointmentViewsPort;
import com.ozin.application.customer.port.out.LoadCustomerPort;
import com.ozin.application.shared.PageQuery;
import com.ozin.application.shared.PageResult;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.ResourceNotFoundException;

public final class ListCustomerAppointmentsService implements ListCustomerAppointmentsUseCase {

    private final LoadCustomerPort customers;
    private final LoadAppointmentViewsPort views;

    public ListCustomerAppointmentsService(LoadCustomerPort customers, LoadAppointmentViewsPort views) {
        this.customers = customers;
        this.views = views;
    }

    @Override
    public PageResult<AppointmentView> list(CustomerId customerId, PageQuery page) {
        if (!customers.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente", customerId.value());
        }
        return views.findByCustomer(customerId, page);
    }
}

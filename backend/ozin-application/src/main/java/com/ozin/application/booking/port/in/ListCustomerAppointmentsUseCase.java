package com.ozin.application.booking.port.in;

import com.ozin.application.booking.AppointmentView;
import com.ozin.application.shared.PageQuery;
import com.ozin.application.shared.PageResult;
import com.ozin.domain.customer.CustomerId;

/** HU-04. */
public interface ListCustomerAppointmentsUseCase {

    PageResult<AppointmentView> list(CustomerId customerId, PageQuery page);
}

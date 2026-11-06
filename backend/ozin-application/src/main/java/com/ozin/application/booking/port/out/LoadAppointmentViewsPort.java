package com.ozin.application.booking.port.out;

import com.ozin.application.booking.AgendaRange;
import com.ozin.application.booking.AppointmentView;
import com.ozin.application.shared.PageQuery;
import com.ozin.application.shared.PageResult;
import com.ozin.domain.branch.BranchId;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.staff.StylistId;

import java.util.List;

public interface LoadAppointmentViewsPort {

    /** Ordenadas de la más reciente a la más antigua. */
    PageResult<AppointmentView> findByCustomer(CustomerId customerId, PageQuery page);

    /** Ordenadas cronológicamente. */
    List<AppointmentView> findByStylist(StylistId stylistId, AgendaRange range);

    /** Ordenadas cronológicamente. */
    List<AppointmentView> findByBranch(BranchId branchId, AgendaRange range);
}

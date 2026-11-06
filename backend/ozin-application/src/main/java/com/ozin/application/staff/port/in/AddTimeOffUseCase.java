package com.ozin.application.staff.port.in;

import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;

/** HU-12. */
public interface AddTimeOffUseCase {

    Stylist addTimeOff(StylistId stylistId, TimeOff timeOff);
}

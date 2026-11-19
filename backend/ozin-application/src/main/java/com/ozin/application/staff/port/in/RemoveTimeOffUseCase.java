package com.ozin.application.staff.port.in;

import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.time.Instant;

/** HU-12: corrige una ausencia registrada por error o que ya no aplica. */
public interface RemoveTimeOffUseCase {

    Stylist removeTimeOff(StylistId stylistId, Instant start);
}

package com.ozin.application.staff.port.out;

import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.util.List;
import java.util.Optional;

public interface LoadStylistPort {

    Optional<Stylist> findById(StylistId id);

    List<Stylist> findAll(boolean includeInactive);
}

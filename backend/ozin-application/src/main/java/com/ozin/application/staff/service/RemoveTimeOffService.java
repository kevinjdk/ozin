package com.ozin.application.staff.service;

import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.RemoveTimeOffUseCase;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;

import java.time.Instant;

public final class RemoveTimeOffService implements RemoveTimeOffUseCase {

    private final LoadStylistPort loadStylists;
    private final SaveStylistPort saveStylists;
    private final UnitOfWork unitOfWork;

    public RemoveTimeOffService(LoadStylistPort loadStylists, SaveStylistPort saveStylists, UnitOfWork unitOfWork) {
        this.loadStylists = loadStylists;
        this.saveStylists = saveStylists;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Stylist removeTimeOff(StylistId stylistId, Instant start) {
        return unitOfWork.inTransaction(() -> {
            Stylist stylist = loadStylists.findById(stylistId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estilista", stylistId.value()));
            stylist.removeTimeOff(start);
            saveStylists.save(stylist);
            return stylist;
        });
    }
}

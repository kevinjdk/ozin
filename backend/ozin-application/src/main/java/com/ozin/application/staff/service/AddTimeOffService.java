package com.ozin.application.staff.service;

import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.application.staff.port.in.AddTimeOffUseCase;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.application.staff.port.out.SaveStylistPort;
import com.ozin.domain.shared.ResourceNotFoundException;
import com.ozin.domain.staff.Stylist;
import com.ozin.domain.staff.StylistId;
import com.ozin.domain.staff.TimeOff;

public final class AddTimeOffService implements AddTimeOffUseCase {

    private final LoadStylistPort loadStylists;
    private final SaveStylistPort saveStylists;
    private final UnitOfWork unitOfWork;

    public AddTimeOffService(LoadStylistPort loadStylists, SaveStylistPort saveStylists, UnitOfWork unitOfWork) {
        this.loadStylists = loadStylists;
        this.saveStylists = saveStylists;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Stylist addTimeOff(StylistId stylistId, TimeOff timeOff) {
        return unitOfWork.inTransaction(() -> {
            Stylist stylist = loadStylists.findById(stylistId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estilista", stylistId.value()));
            stylist.addTimeOff(timeOff);
            saveStylists.save(stylist);
            return stylist;
        });
    }
}

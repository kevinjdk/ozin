package com.ozin.application.staff.service;

import com.ozin.application.staff.port.in.ListStylistsUseCase;
import com.ozin.application.staff.port.out.LoadStylistPort;
import com.ozin.domain.catalog.SalonServiceId;
import com.ozin.domain.staff.Stylist;

import java.util.List;
import java.util.Optional;

public final class ListStylistsService implements ListStylistsUseCase {

    private final LoadStylistPort stylists;

    public ListStylistsService(LoadStylistPort stylists) {
        this.stylists = stylists;
    }

    @Override
    public List<Stylist> list(Optional<SalonServiceId> serviceId, boolean includeInactive) {
        return stylists.findAll(includeInactive).stream()
                .filter(stylist -> serviceId.map(stylist::canPerform).orElse(true))
                .toList();
    }
}

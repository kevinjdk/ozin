package com.ozin.application.branch.service;

import com.ozin.application.branch.port.in.ListBranchesUseCase;
import com.ozin.application.branch.port.out.LoadBranchPort;
import com.ozin.domain.branch.Branch;

import java.util.List;

public final class ListBranchesService implements ListBranchesUseCase {

    private final LoadBranchPort branches;

    public ListBranchesService(LoadBranchPort branches) {
        this.branches = branches;
    }

    @Override
    public List<Branch> listActive() {
        return branches.findAllActive();
    }
}

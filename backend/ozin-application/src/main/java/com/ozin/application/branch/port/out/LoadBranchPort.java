package com.ozin.application.branch.port.out;

import com.ozin.domain.branch.Branch;
import com.ozin.domain.branch.BranchId;

import java.util.List;
import java.util.Optional;

public interface LoadBranchPort {

    Optional<Branch> findById(BranchId id);

    List<Branch> findAllActive();
}

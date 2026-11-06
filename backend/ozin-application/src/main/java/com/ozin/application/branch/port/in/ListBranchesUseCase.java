package com.ozin.application.branch.port.in;

import com.ozin.domain.branch.Branch;

import java.util.List;

public interface ListBranchesUseCase {

    List<Branch> listActive();
}

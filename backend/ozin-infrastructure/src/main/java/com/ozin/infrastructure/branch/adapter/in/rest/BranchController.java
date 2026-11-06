package com.ozin.infrastructure.branch.adapter.in.rest;

import com.ozin.application.branch.port.in.ListBranchesUseCase;
import com.ozin.domain.branch.Branch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Sedes")
class BranchController {

    private final ListBranchesUseCase listBranches;

    BranchController(ListBranchesUseCase listBranches) {
        this.listBranches = listBranches;
    }

    @GetMapping
    @Operation(summary = "Lista las sedes activas")
    List<BranchResponse> list() {
        return listBranches.listActive().stream().map(BranchResponse::from).toList();
    }

    record BranchResponse(UUID id, String name, String address, String timeZone) {

        static BranchResponse from(Branch branch) {
            return new BranchResponse(branch.id().value(), branch.name(), branch.address(), branch.timeZone().getId());
        }
    }
}

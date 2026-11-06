package com.ozin.application.identity.service;

import com.ozin.application.identity.port.in.EnsureAdminUseCase;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.PasswordHasherPort;
import com.ozin.application.identity.port.out.SaveUserPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;
import com.ozin.domain.identity.Role;
import com.ozin.domain.identity.User;
import com.ozin.domain.identity.UserId;

public final class EnsureAdminService implements EnsureAdminUseCase {

    private final LoadUserPort loadUsers;
    private final SaveUserPort saveUsers;
    private final PasswordHasherPort hasher;
    private final UnitOfWork unitOfWork;

    public EnsureAdminService(LoadUserPort loadUsers,
                              SaveUserPort saveUsers,
                              PasswordHasherPort hasher,
                              UnitOfWork unitOfWork) {
        this.loadUsers = loadUsers;
        this.saveUsers = saveUsers;
        this.hasher = hasher;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void ensureAdmin(Email email, RawPassword password) {
        unitOfWork.inTransaction(() -> {
            if (!loadUsers.existsByEmail(email)) {
                saveUsers.save(new User(UserId.newId(), email, hasher.hash(password), Role.ADMIN, null, true));
            }
            return null;
        });
    }
}

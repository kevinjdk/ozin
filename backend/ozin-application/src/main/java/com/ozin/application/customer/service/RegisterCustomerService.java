package com.ozin.application.customer.service;

import com.ozin.application.customer.port.in.RegisterCustomerUseCase;
import com.ozin.application.customer.port.out.SaveCustomerPort;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.PasswordHasherPort;
import com.ozin.application.identity.port.out.SaveUserPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.identity.Role;
import com.ozin.domain.identity.User;
import com.ozin.domain.identity.UserId;
import com.ozin.domain.shared.BusinessRuleViolationException;

public final class RegisterCustomerService implements RegisterCustomerUseCase {

    private final LoadUserPort loadUsers;
    private final SaveUserPort saveUsers;
    private final SaveCustomerPort saveCustomers;
    private final PasswordHasherPort hasher;
    private final UnitOfWork unitOfWork;

    public RegisterCustomerService(LoadUserPort loadUsers,
                                   SaveUserPort saveUsers,
                                   SaveCustomerPort saveCustomers,
                                   PasswordHasherPort hasher,
                                   UnitOfWork unitOfWork) {
        this.loadUsers = loadUsers;
        this.saveUsers = saveUsers;
        this.saveCustomers = saveCustomers;
        this.hasher = hasher;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Customer register(Command command) {
        Customer customer = new Customer(CustomerId.newId(), command.fullName(), command.email(), command.phone());
        // BCrypt es lento a propósito: se calcula antes de abrir la transacción para no retener una conexión.
        String passwordHash = hasher.hash(command.password());
        return unitOfWork.inTransaction(() -> {
            if (loadUsers.existsByEmail(command.email())) {
                throw new BusinessRuleViolationException(Customer.EMAIL_ALREADY_REGISTERED, "El email ya está registrado");
            }
            saveCustomers.save(customer);
            saveUsers.save(new User(UserId.newId(), command.email(), passwordHash, Role.CUSTOMER,
                    customer.id().value(), true));
            return customer;
        });
    }
}

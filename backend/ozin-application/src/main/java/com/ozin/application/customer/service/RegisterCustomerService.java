package com.ozin.application.customer.service;

import com.ozin.application.customer.port.in.RegisterCustomerUseCase;
import com.ozin.application.customer.port.out.LoadCustomerPort;
import com.ozin.application.customer.port.out.SaveCustomerPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.BusinessRuleViolationException;

public final class RegisterCustomerService implements RegisterCustomerUseCase {

    private final LoadCustomerPort loadCustomers;
    private final SaveCustomerPort saveCustomers;
    private final UnitOfWork unitOfWork;

    public RegisterCustomerService(LoadCustomerPort loadCustomers,
                                   SaveCustomerPort saveCustomers,
                                   UnitOfWork unitOfWork) {
        this.loadCustomers = loadCustomers;
        this.saveCustomers = saveCustomers;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Customer register(Command command) {
        Customer customer = new Customer(CustomerId.newId(), command.fullName(), command.email(), command.phone());
        return unitOfWork.inTransaction(() -> {
            if (loadCustomers.existsByEmail(command.email())) {
                throw new BusinessRuleViolationException(Customer.EMAIL_ALREADY_REGISTERED, "El email ya está registrado");
            }
            saveCustomers.save(customer);
            return customer;
        });
    }
}

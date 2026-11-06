package com.ozin.infrastructure.customer.adapter.out.persistence;

import com.ozin.application.customer.port.out.LoadCustomerPort;
import com.ozin.application.customer.port.out.SaveCustomerPort;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.CustomerId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class CustomerPersistenceAdapter implements LoadCustomerPort, SaveCustomerPort {

    private static final String EMAIL_CONSTRAINT = "customer_email_unique";

    private final CustomerJpaRepository repository;

    CustomerPersistenceAdapter(CustomerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsById(CustomerId id) {
        return repository.existsById(id.value());
    }

    @Override
    public boolean lockForBooking(CustomerId id) {
        return repository.findByIdForUpdate(id.value()).isPresent();
    }

    @Override
    public void save(Customer customer) {
        try {
            repository.saveAndFlush(new CustomerJpaEntity(customer.id().value(), customer.fullName(),
                    customer.email().value(), customer.phone().value()));
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(EMAIL_CONSTRAINT)) {
                throw new BusinessRuleViolationException(Customer.EMAIL_ALREADY_REGISTERED, "El email ya está registrado");
            }
            throw ex;
        }
    }
}

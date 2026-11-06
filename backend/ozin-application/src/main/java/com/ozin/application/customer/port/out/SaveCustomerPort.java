package com.ozin.application.customer.port.out;

import com.ozin.domain.customer.Customer;

public interface SaveCustomerPort {

    /** @throws com.ozin.domain.shared.BusinessRuleViolationException si otro cliente registró el mismo email a la vez */
    void save(Customer customer);
}

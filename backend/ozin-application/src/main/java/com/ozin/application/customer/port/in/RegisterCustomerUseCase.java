package com.ozin.application.customer.port.in;

import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.Email;
import com.ozin.domain.customer.PhoneNumber;

/** Alta de cliente. En la Fase 4 se integrará con el registro de usuario (HU-06). */
public interface RegisterCustomerUseCase {

    Customer register(Command command);

    record Command(String fullName, Email email, PhoneNumber phone) {
    }
}

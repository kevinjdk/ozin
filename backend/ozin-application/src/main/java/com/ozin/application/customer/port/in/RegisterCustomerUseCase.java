package com.ozin.application.customer.port.in;

import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.Email;
import com.ozin.domain.customer.PhoneNumber;
import com.ozin.domain.identity.RawPassword;

/** HU-06: crea el cliente junto con sus credenciales de acceso. */
public interface RegisterCustomerUseCase {

    Customer register(Command command);

    record Command(String fullName, Email email, PhoneNumber phone, RawPassword password) {
    }
}

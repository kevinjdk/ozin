package com.ozin.application.identity.port.out;

import com.ozin.domain.identity.User;

public interface SaveUserPort {

    /** @throws com.ozin.domain.shared.BusinessRuleViolationException si otro usuario registró el mismo email a la vez */
    void save(User user);
}

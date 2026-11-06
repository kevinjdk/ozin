package com.ozin.application.identity.port.out;

import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.User;
import com.ozin.domain.identity.UserId;
import com.ozin.domain.staff.StylistId;

import java.util.Optional;

public interface LoadUserPort {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByStylist(StylistId stylistId);

    boolean existsByEmail(Email email);
}

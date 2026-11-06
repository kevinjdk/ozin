package com.ozin.application.identity.port.in;

import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;

/** Crea el administrador inicial si todavía no existe un usuario con ese email. */
public interface EnsureAdminUseCase {

    void ensureAdmin(Email email, RawPassword password);
}

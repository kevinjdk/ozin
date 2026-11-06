package com.ozin.application.identity.port.in;

/** Cierra la sesión asociada al token. Es idempotente: un token desconocido no produce error. */
public interface LogoutUseCase {

    void logout(String refreshToken);
}

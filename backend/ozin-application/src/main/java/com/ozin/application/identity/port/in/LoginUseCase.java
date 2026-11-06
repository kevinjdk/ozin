package com.ozin.application.identity.port.in;

import com.ozin.application.identity.AuthTokens;

/** HU-06. */
public interface LoginUseCase {

    AuthTokens login(Command command);

    /** @param clientAddress dirección de origen, usada para limitar los intentos fallidos */
    record Command(String email, String password, String clientAddress) {

        @Override
        public String toString() {
            return "LoginUseCase.Command[***]";
        }
    }
}

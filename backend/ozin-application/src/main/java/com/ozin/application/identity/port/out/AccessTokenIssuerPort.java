package com.ozin.application.identity.port.out;

import com.ozin.domain.identity.User;

public interface AccessTokenIssuerPort {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}

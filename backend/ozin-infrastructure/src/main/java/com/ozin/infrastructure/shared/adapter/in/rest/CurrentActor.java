package com.ozin.infrastructure.shared.adapter.in.rest;

import com.ozin.domain.identity.Actor;
import com.ozin.domain.identity.Role;
import com.ozin.infrastructure.config.SecurityProperties;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Traduce el token validado al concepto de negocio {@link Actor}. La identidad nunca sale del cuerpo de la petición. */
public final class CurrentActor {

    private CurrentActor() {
    }

    public static Actor from(Jwt jwt) {
        String profileId = jwt.getClaimAsString(SecurityProperties.PROFILE_CLAIM);
        return new Actor(Role.valueOf(jwt.getClaimAsString(SecurityProperties.ROLE_CLAIM)),
                profileId == null ? null : UUID.fromString(profileId));
    }
}

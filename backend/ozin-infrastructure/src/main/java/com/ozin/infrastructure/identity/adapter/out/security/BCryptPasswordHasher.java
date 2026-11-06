package com.ozin.infrastructure.identity.adapter.out.security;

import com.ozin.application.identity.port.out.PasswordHasherPort;
import com.ozin.domain.identity.RawPassword;
import com.ozin.infrastructure.config.SecurityProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
class BCryptPasswordHasher implements PasswordHasherPort {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final BCryptPasswordEncoder encoder;
    private final String decoyHash;

    BCryptPasswordHasher(SecurityProperties properties) {
        this.encoder = new BCryptPasswordEncoder(properties.bcryptStrength());
        this.decoyHash = encoder.encode("decoy-password-for-constant-time");
    }

    @Override
    public String hash(RawPassword password) {
        return encoder.encode(password.value());
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(truncateToBcryptLimit(rawPassword), passwordHash)
                && rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    @Override
    public void simulateVerification(String rawPassword) {
        encoder.matches(truncateToBcryptLimit(rawPassword), decoyHash);
    }

    /** BCrypt solo admite 72 bytes; una entrada mayor nunca es válida, pero debe costar el mismo tiempo. */
    private static String truncateToBcryptLimit(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES
                ? rawPassword
                : rawPassword.substring(0, Math.min(rawPassword.length(), BCRYPT_MAX_BYTES / 4));
    }
}

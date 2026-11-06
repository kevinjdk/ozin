package com.ozin.application.identity.port.out;

/** Cuenta los inicios de sesión fallidos para frenar ataques de fuerza bruta. */
public interface LoginThrottlePort {

    boolean isBlocked(String key);

    void recordFailure(String key);

    void reset(String key);
}

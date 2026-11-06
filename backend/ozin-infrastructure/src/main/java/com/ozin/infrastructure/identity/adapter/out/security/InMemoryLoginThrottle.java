package com.ozin.infrastructure.identity.adapter.out.security;

import com.ozin.application.identity.SessionPolicy;
import com.ozin.application.identity.port.out.LoginThrottlePort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador en memoria: suficiente para una sola instancia de la API.
 * Con varias instancias, cada una contaría por separado; bastaría otra implementación del puerto (por ejemplo, Redis).
 */
@Component
class InMemoryLoginThrottle implements LoginThrottlePort {

    private static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final SessionPolicy policy;
    private final Clock clock;

    InMemoryLoginThrottle(SessionPolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public boolean isBlocked(String key) {
        Attempts current = attempts.get(key);
        return current != null && !isExpired(current, clock.instant()) && current.failures() >= policy.maxFailedLogins();
    }

    @Override
    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (attempts.size() >= PURGE_THRESHOLD) {
            attempts.values().removeIf(entry -> isExpired(entry, now));
        }
        attempts.compute(key, (ignored, current) -> current == null || isExpired(current, now)
                ? new Attempts(1, now)
                : new Attempts(current.failures() + 1, current.firstFailure()));
    }

    @Override
    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts entry, Instant now) {
        return !now.isBefore(entry.firstFailure().plus(policy.failedLoginWindow()));
    }

    private record Attempts(int failures, Instant firstFailure) {
    }
}

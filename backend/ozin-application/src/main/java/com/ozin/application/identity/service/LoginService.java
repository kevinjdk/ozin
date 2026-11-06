package com.ozin.application.identity.service;

import com.ozin.application.identity.AuthTokens;
import com.ozin.application.identity.port.in.LoginUseCase;
import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.LoginThrottlePort;
import com.ozin.application.identity.port.out.PasswordHasherPort;
import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.application.shared.port.out.UnitOfWork;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.User;
import com.ozin.domain.shared.InvalidCredentialsException;
import com.ozin.domain.shared.TooManyAttemptsException;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class LoginService implements LoginUseCase {

    private final LoadUserPort users;
    private final PasswordHasherPort hasher;
    private final LoginThrottlePort throttle;
    private final RefreshTokenPort refreshTokens;
    private final SessionTokenService sessions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LoginService(LoadUserPort users,
                        PasswordHasherPort hasher,
                        LoginThrottlePort throttle,
                        RefreshTokenPort refreshTokens,
                        SessionTokenService sessions,
                        UnitOfWork unitOfWork,
                        Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.throttle = throttle;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public AuthTokens login(Command command) {
        Email email = new Email(command.email());
        // Combinar email e IP evita que un tercero bloquee la cuenta de otra persona desde su propia dirección.
        String throttleKey = email.value() + "|" + command.clientAddress();
        if (throttle.isBlocked(throttleKey)) {
            throw new TooManyAttemptsException();
        }

        Optional<User> authenticated = authenticate(email, command.password());
        if (authenticated.isEmpty()) {
            throttle.recordFailure(throttleKey);
            throw new InvalidCredentialsException("INVALID_CREDENTIALS", "Email o contraseña incorrectos");
        }

        throttle.reset(throttleKey);
        User user = authenticated.get();
        return unitOfWork.inTransaction(() -> {
            refreshTokens.deleteExpiredOf(user.id(), clock.instant());
            return sessions.issue(user, UUID.randomUUID());
        });
    }

    private Optional<User> authenticate(Email email, String password) {
        Optional<User> user = users.findByEmail(email);
        if (user.isEmpty()) {
            hasher.simulateVerification(password);
            return Optional.empty();
        }
        boolean valid = hasher.matches(password, user.get().passwordHash());
        return valid && user.get().isEnabled() ? user : Optional.empty();
    }
}

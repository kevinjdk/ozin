package com.ozin.infrastructure.identity.adapter.out.persistence;

import com.ozin.application.identity.port.out.LoadUserPort;
import com.ozin.application.identity.port.out.SaveUserPort;
import com.ozin.domain.customer.Customer;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.Role;
import com.ozin.domain.identity.User;
import com.ozin.domain.identity.UserId;
import com.ozin.domain.shared.BusinessRuleViolationException;
import com.ozin.domain.staff.StylistId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private static final String EMAIL_CONSTRAINT = "app_user_email_unique";

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByStylist(StylistId stylistId) {
        return repository.findByStylistId(stylistId.value()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public void save(User user) {
        UserJpaEntity entity = repository.findById(user.id().value()).orElseGet(() -> newEntity(user));
        entity.passwordHash = user.passwordHash();
        entity.enabled = user.isEnabled();
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(EMAIL_CONSTRAINT)) {
                throw new BusinessRuleViolationException(Customer.EMAIL_ALREADY_REGISTERED, "El email ya está registrado");
            }
            throw ex;
        }
    }

    private static UserJpaEntity newEntity(User user) {
        return new UserJpaEntity(user.id().value(), user.email().value(), user.role(),
                user.role() == Role.CUSTOMER ? user.profileId() : null,
                user.role() == Role.STYLIST ? user.profileId() : null);
    }

    private static User toDomain(UserJpaEntity entity) {
        return new User(UserId.of(entity.id), new Email(entity.email), entity.passwordHash, entity.role,
                entity.role == Role.CUSTOMER ? entity.customerId : entity.stylistId, entity.enabled);
    }
}

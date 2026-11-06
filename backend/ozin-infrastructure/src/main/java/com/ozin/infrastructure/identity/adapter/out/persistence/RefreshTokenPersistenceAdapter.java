package com.ozin.infrastructure.identity.adapter.out.persistence;

import com.ozin.application.identity.port.out.RefreshTokenPort;
import com.ozin.domain.identity.RefreshToken;
import com.ozin.domain.identity.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenJpaRepository repository;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(RefreshToken token) {
        RefreshTokenJpaEntity entity = repository.findById(token.id())
                .orElseGet(() -> new RefreshTokenJpaEntity(token.id(), token.userId().value(), token.familyId(),
                        token.tokenHash(), token.expiresAt()));
        entity.revokedAt = token.revokedAt();
        repository.saveAndFlush(entity);
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(entity -> new RefreshToken(entity.id,
                UserId.of(entity.userId), entity.familyId, entity.tokenHash, entity.expiresAt, entity.revokedAt));
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        repository.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllOf(UserId userId, Instant now) {
        repository.revokeAllOfUser(userId.value(), now);
    }

    @Override
    public void deleteExpiredOf(UserId userId, Instant now) {
        repository.deleteExpiredOfUser(userId.value(), now);
    }
}

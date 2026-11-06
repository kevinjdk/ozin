package com.ozin.infrastructure.shared.adapter.out.persistence;

import com.ozin.application.shared.port.out.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Implementa {@link UnitOfWork} con el gestor de transacciones de Spring (ADR-0007). */
@Component
class TransactionalUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    TransactionalUnitOfWork(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}

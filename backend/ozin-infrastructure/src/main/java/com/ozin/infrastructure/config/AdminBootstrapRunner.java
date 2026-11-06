package com.ozin.infrastructure.config;

import com.ozin.application.identity.port.in.EnsureAdminUseCase;
import com.ozin.domain.customer.Email;
import com.ozin.domain.identity.RawPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el administrador inicial a partir de variables de entorno. El registro público
 * solo crea clientes, y el repositorio no contiene ninguna credencial por defecto.
 */
@Component
class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final EnsureAdminUseCase ensureAdmin;
    private final SecurityProperties properties;

    AdminBootstrapRunner(EnsureAdminUseCase ensureAdmin, SecurityProperties properties) {
        this.ensureAdmin = ensureAdmin;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(properties.adminEmail()) || isBlank(properties.adminPassword())) {
            log.info("OZIN_ADMIN_EMAIL u OZIN_ADMIN_PASSWORD sin definir: no se crea un administrador inicial");
            return;
        }
        ensureAdmin.ensureAdmin(new Email(properties.adminEmail()), new RawPassword(properties.adminPassword()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

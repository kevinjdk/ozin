# Ozin · Sistema de reservas para peluquerías

[![CI](https://github.com/kevinjdk/ozin/actions/workflows/ci.yml/badge.svg)](https://github.com/kevinjdk/ozin/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![Angular](https://img.shields.io/badge/Angular-21-DD0031)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)

Ozin permite a los clientes de una peluquería reservar citas en línea, a los estilistas gestionar su agenda
y a los administradores controlar servicios, horarios y estilistas.

El proyecto está construido con **arquitectura hexagonal**, **principios SOLID** y **TDD**, con foco en la
calidad, la seguridad y la mantenibilidad propias de un sistema empresarial.

> 🚧 En desarrollo. Ver la [hoja de ruta](#hoja-de-ruta). Las notificaciones todavía solo se registran en el log.

## Seguridad

- Access token de corta duración en memoria y refresh token rotativo en una cookie `HttpOnly`, con detección de reutilización y protección CSRF.
- Autorización por rol en cada ruta y por propietario en cada recurso, con denegación por defecto.
- Contraseñas con BCrypt, límite de intentos de inicio de sesión y respuestas que no revelan qué cuentas existen.
- Sin credenciales en el repositorio: sin `OZIN_JWT_SECRET` la aplicación no arranca.

Las decisiones están en los ADR [0004](docs/adr/0004-autenticacion-jwt.md) y [0008](docs/adr/0008-refresh-token-en-cookie.md), y se comprueban en
[SecurityApiTests](backend/ozin-bootstrap/src/test/java/com/ozin/SecurityApiTests.java).

## Stack

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), Flyway, springdoc-openapi |
| Frontend | Angular 21 (standalone, signals), PrimeNG 21 |
| Base de datos | PostgreSQL 17 |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit, Vitest, Playwright |
| DevOps | Docker, GitHub Actions, JaCoCo |

## Arquitectura

```
ozin/
├── backend/                  Maven multimódulo (arquitectura hexagonal)
│   ├── ozin-domain/          Java puro: entidades, value objects, reglas de negocio
│   ├── ozin-application/     Casos de uso y puertos
│   ├── ozin-infrastructure/  Adaptadores: REST, JPA, seguridad, email
│   └── ozin-bootstrap/       Arranque de Spring Boot, configuración y migraciones
├── frontend/                 Angular + PrimeNG
└── docs/                     Requisitos, arquitectura y ADRs
```

Las dependencias apuntan siempre hacia el dominio. Se garantizan **al compilar** (módulos Maven)
y **en los tests** (ArchUnit).

- [Arquitectura hexagonal y SOLID](docs/architecture/hexagonal-architecture.md)
- [Modelo de dominio](docs/architecture/domain-model.md)
- [Reglas de negocio](docs/requirements/business-rules.md)
- [Historias de usuario](docs/requirements/user-stories.md)
- [Decisiones de arquitectura (ADRs)](docs/adr/README.md)

## Cómo ejecutarlo

**Requisitos:** JDK 21, Node 24, Docker.

```bash
# 1. Base de datos
docker compose up -d

# 2. Backend  →  http://localhost:8080/swagger-ui.html
export OZIN_JWT_SECRET=$(openssl rand -base64 48)    # Obligatorio
export OZIN_ADMIN_EMAIL=admin@example.com            # Administrador inicial (opcional)
export OZIN_ADMIN_PASSWORD='elige-una-Clave1'
cd backend
./mvnw -DskipTests package
java -jar ozin-bootstrap/target/ozin-api.jar

# 3. Frontend  →  http://localhost:4200
cd frontend
npm install
npm start
```

### Tests

```bash
cd backend && ./mvnw verify      # unitarios + arquitectura + integración (requiere Docker)
cd frontend && npm run test:ci

# Recorridos E2E: con la base de datos y el backend en marcha, con las variables del paso 2
cd frontend && npx playwright install chromium && npm run e2e
```

## Hoja de ruta

- [x] **Fase 0:** requisitos, modelo de dominio y ADRs
- [x] **Fase 1:** monorepo, Maven multimódulo, Angular + PrimeNG, Docker y CI
- [x] **Fase 2:** dominio de reservas con TDD
- [x] **Fase 3:** persistencia (JPA + Flyway) y API REST
- [x] **Fase 4:** seguridad (JWT, roles)
- [x] **Fase 5:** frontend (reserva, panel de cliente, estilista y admin)
- [ ] **Fase 6:** despliegue y SonarCloud

## Autor

**Kevin Holguin** · [github.com/kevinjdk](https://github.com/kevinjdk)

## Licencia

[MIT](LICENSE)

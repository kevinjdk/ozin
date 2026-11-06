# Ozin · Sistema de reservas para peluquerías

[![CI](https://github.com/kevinjdk/ozin/actions/workflows/ci.yml/badge.svg)](https://github.com/kevinjdk/ozin/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![Angular](https://img.shields.io/badge/Angular-22-DD0031)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791)

Ozin permite a los clientes de una peluquería reservar citas en línea, a los estilistas gestionar su agenda
y a los administradores controlar servicios, horarios y estilistas.

El proyecto está construido con **arquitectura hexagonal**, **principios SOLID** y **TDD**, con foco en la
calidad, la seguridad y la mantenibilidad propias de un sistema empresarial.

> 🚧 En desarrollo. Ver la [hoja de ruta](#hoja-de-ruta). La API todavía no tiene autenticación (llega en la
> Fase 4) y las notificaciones solo se registran en el log, por lo que aún no debe exponerse públicamente.

## Stack

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), Flyway, springdoc-openapi |
| Frontend | Angular 22 (standalone, signals), PrimeNG 22 |
| Base de datos | PostgreSQL 17 |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit, Vitest |
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
```

## Hoja de ruta

- [x] **Fase 0:** requisitos, modelo de dominio y ADRs
- [x] **Fase 1:** monorepo, Maven multimódulo, Angular + PrimeNG, Docker y CI
- [x] **Fase 2:** dominio de reservas con TDD
- [x] **Fase 3:** persistencia (JPA + Flyway) y API REST
- [ ] **Fase 4:** seguridad (JWT, roles)
- [ ] **Fase 5:** frontend (reserva, panel de cliente, estilista y admin)
- [ ] **Fase 6:** despliegue y SonarCloud

## Autor

**Kevin Holguin** · [github.com/kevinjdk](https://github.com/kevinjdk)

## Licencia

[MIT](LICENSE)

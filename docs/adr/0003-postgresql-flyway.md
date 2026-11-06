# ADR-0003: PostgreSQL con migraciones Flyway

- **Estado:** Aceptado
- **Fecha:** 2026-11-05

## Contexto
Se necesita integridad fuerte para evitar citas solapadas incluso bajo concurrencia,
y un historial auditable de cambios del esquema.

## Decisión
- **PostgreSQL** como base de datos, usando `timestamptz` y una restricción
  `EXCLUDE USING gist` (extensión `btree_gist`) para impedir solapamientos de citas activas del mismo estilista.
- **Flyway** para migraciones versionadas; `spring.jpa.hibernate.ddl-auto=validate` (Hibernate nunca modifica el esquema).
- **Bloqueo optimista** (`@Version`) en entidades que se modifican concurrentemente.

## Consecuencias
- (+) Defensa en profundidad: la regla RN-01 se valida en el dominio y en la base de datos.
- (+) Esquema reproducible en cualquier entorno.
- (−) Dependencia de una característica específica de PostgreSQL.

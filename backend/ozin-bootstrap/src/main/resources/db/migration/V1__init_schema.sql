-- =====================================================================
-- V1: Esquema inicial
-- btree_gist permite combinar igualdad (stylist_id) con rangos de tiempo
-- en restricciones EXCLUDE; se usará para impedir citas solapadas (RN-01).
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Sedes (ADR-0006)
CREATE TABLE branch (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    time_zone   VARCHAR(64)  NOT NULL DEFAULT 'America/Guayaquil',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Sede inicial del MVP
INSERT INTO branch (id, name, address, time_zone)
VALUES ('00000000-0000-0000-0000-000000000001', 'Ozin Matriz', 'Por definir', 'America/Guayaquil');

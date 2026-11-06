-- =====================================================================
-- V3: Usuarios y sesiones (ADR-0004)
-- =====================================================================

-- 'user' es palabra reservada en PostgreSQL.
CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'STYLIST', 'CUSTOMER')),
    customer_id   UUID UNIQUE REFERENCES customer (id),
    stylist_id    UUID UNIQUE REFERENCES stylist (id),
    enabled       BOOLEAN      NOT NULL,
    version       BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT app_user_email_unique UNIQUE (email),
    CONSTRAINT app_user_profile_matches_role CHECK (
        (role = 'ADMIN'    AND customer_id IS NULL     AND stylist_id IS NULL) OR
        (role = 'CUSTOMER' AND customer_id IS NOT NULL AND stylist_id IS NULL) OR
        (role = 'STYLIST'  AND stylist_id IS NOT NULL  AND customer_id IS NULL)
    )
);

-- Solo se guarda el hash del token: una filtración de esta tabla no permite suplantar sesiones.
CREATE TABLE refresh_token (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    family_id  UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);
CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);

-- =====================================================================
-- V2: Catálogo, clientes, estilistas y citas
-- =====================================================================

CREATE TABLE salon_service (
    id               UUID          PRIMARY KEY,
    name             VARCHAR(120)  NOT NULL,
    duration_minutes INTEGER       NOT NULL CHECK (duration_minutes BETWEEN 1 AND 480),
    price            NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    currency         VARCHAR(3)    NOT NULL,
    active           BOOLEAN       NOT NULL,
    version          BIGINT        NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);

CREATE TABLE customer (
    id         UUID         PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    phone      VARCHAR(16)  NOT NULL,
    version    BIGINT       NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT customer_email_unique UNIQUE (email)
);

CREATE TABLE stylist (
    id         UUID         PRIMARY KEY,
    branch_id  UUID         NOT NULL REFERENCES branch (id),
    full_name  VARCHAR(150) NOT NULL,
    active     BOOLEAN      NOT NULL,
    version    BIGINT       NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE stylist_service (
    stylist_id UUID NOT NULL REFERENCES stylist (id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES salon_service (id),
    PRIMARY KEY (stylist_id, service_id)
);

CREATE TABLE working_hours (
    stylist_id  UUID        NOT NULL REFERENCES stylist (id) ON DELETE CASCADE,
    day_of_week VARCHAR(9)  NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    PRIMARY KEY (stylist_id, day_of_week, start_time),
    CHECK (start_time < end_time)
);

CREATE TABLE time_off (
    stylist_id UUID         NOT NULL REFERENCES stylist (id) ON DELETE CASCADE,
    starts_at  TIMESTAMPTZ  NOT NULL,
    ends_at    TIMESTAMPTZ  NOT NULL,
    reason     VARCHAR(255) NOT NULL,
    PRIMARY KEY (stylist_id, starts_at),
    CHECK (starts_at < ends_at)
);

CREATE TABLE appointment (
    id           UUID          PRIMARY KEY,
    customer_id  UUID          NOT NULL REFERENCES customer (id),
    stylist_id   UUID          NOT NULL REFERENCES stylist (id),
    service_id   UUID          NOT NULL REFERENCES salon_service (id),
    branch_id    UUID          NOT NULL REFERENCES branch (id),
    starts_at    TIMESTAMPTZ   NOT NULL,
    ends_at      TIMESTAMPTZ   NOT NULL,
    price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    currency     VARCHAR(3)    NOT NULL,
    status       VARCHAR(16)   NOT NULL
        CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    cancelled_by VARCHAR(16),
    version      BIGINT        NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    CHECK (starts_at < ends_at),

    -- RN-01: última línea de defensa frente a reservas simultáneas. Dos citas activas del mismo
    -- estilista no pueden solaparse, aunque ambas transacciones hayan pasado la validación del dominio.
    CONSTRAINT appointment_no_overlap EXCLUDE USING gist (
        stylist_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED'))
);

CREATE INDEX appointment_customer_idx ON appointment (customer_id, starts_at DESC);
CREATE INDEX appointment_branch_idx ON appointment (branch_id, starts_at);
CREATE INDEX appointment_stylist_idx ON appointment (stylist_id, starts_at);

-- =====================================================================
-- V4: Las citas nacen confirmadas y los servicios tienen descripción
-- =====================================================================

-- Ya no existe el estado PENDING: reservar un horario libre basta para asegurarlo.
-- Las citas que estaban pendientes pasan a confirmadas, que es lo que el cliente esperaba.
UPDATE appointment SET status = 'CONFIRMED' WHERE status = 'PENDING';

ALTER TABLE appointment DROP CONSTRAINT appointment_status_check;
ALTER TABLE appointment ADD CONSTRAINT appointment_status_check
    CHECK (status IN ('CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

-- RN-01: se recrea la exclusión con el nuevo conjunto de estados activos.
ALTER TABLE appointment DROP CONSTRAINT appointment_no_overlap;
ALTER TABLE appointment ADD CONSTRAINT appointment_no_overlap EXCLUDE USING gist (
    stylist_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status = 'CONFIRMED');

-- La descripción es opcional; vacía en lugar de nula para que el dominio no tenga que distinguir.
ALTER TABLE salon_service ADD COLUMN description VARCHAR(500) NOT NULL DEFAULT '';

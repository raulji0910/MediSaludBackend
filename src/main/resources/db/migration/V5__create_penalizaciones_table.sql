CREATE TABLE penalizaciones (
    id                  UUID         NOT NULL PRIMARY KEY,
    paciente_id         UUID         NOT NULL REFERENCES pacientes (id),
    cita_id             UUID         REFERENCES citas (id),
    fecha_penalizacion  TIMESTAMP    NOT NULL,
    motivo              VARCHAR(255)
);

CREATE INDEX idx_penalizaciones_paciente_fecha ON penalizaciones (paciente_id, fecha_penalizacion);

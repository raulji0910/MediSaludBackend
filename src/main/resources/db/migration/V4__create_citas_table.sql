CREATE TABLE citas (
    id                 UUID         NOT NULL PRIMARY KEY,
    paciente_id        UUID         NOT NULL REFERENCES pacientes (id),
    medico_id          UUID         NOT NULL REFERENCES medicos (id),
    fecha_hora         TIMESTAMP    NOT NULL,
    estado             VARCHAR(20)  NOT NULL,
    fecha_cancelacion  TIMESTAMP,
    creado_en          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_citas_medico_fecha ON citas (medico_id, fecha_hora);
CREATE INDEX idx_citas_paciente_fecha ON citas (paciente_id, fecha_hora);
CREATE INDEX idx_citas_estado ON citas (estado);

CREATE TABLE medicos (
    id               UUID         NOT NULL PRIMARY KEY,
    nombre_completo  VARCHAR(100) NOT NULL,
    especialidad     VARCHAR(100) NOT NULL,
    telefono         VARCHAR(20),
    email            VARCHAR(150),
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medicos_especialidad ON medicos (especialidad);

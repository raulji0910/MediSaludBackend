CREATE TABLE pacientes (
    id                   UUID         NOT NULL PRIMARY KEY,
    nombre_completo      VARCHAR(100) NOT NULL,
    documento_identidad  VARCHAR(30)  NOT NULL,
    telefono             VARCHAR(20)  NOT NULL,
    email                VARCHAR(150) NOT NULL,
    fecha_nacimiento     DATE,
    creado_en            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_pacientes_documento_identidad ON pacientes (documento_identidad);

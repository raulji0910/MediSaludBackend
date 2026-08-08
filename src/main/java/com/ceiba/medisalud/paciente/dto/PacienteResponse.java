package com.ceiba.medisalud.paciente.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PacienteResponse(
        UUID id,
        String nombreCompleto,
        String documentoIdentidad,
        String telefono,
        String email,
        LocalDate fechaNacimiento,
        Instant creadoEn
) {
}

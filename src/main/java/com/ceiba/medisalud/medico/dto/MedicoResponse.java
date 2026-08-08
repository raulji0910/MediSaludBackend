package com.ceiba.medisalud.medico.dto;

import java.time.Instant;
import java.util.UUID;

public record MedicoResponse(
        UUID id,
        String nombreCompleto,
        String especialidad,
        String telefono,
        String email,
        Instant creadoEn
) {
}

package com.ceiba.medisalud.cita.dto;

import com.ceiba.medisalud.cita.EstadoCita;

import java.time.Instant;
import java.util.UUID;

public record CancelacionResponse(
        UUID citaId,
        EstadoCita estado,
        Instant fechaCancelacion,
        boolean penalizacionRegistrada
) {
}

package com.ceiba.medisalud.cita.dto;

import com.ceiba.medisalud.cita.EstadoCita;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CitaResponse(
        UUID id,
        UUID pacienteId,
        String pacienteNombre,
        UUID medicoId,
        String medicoNombre,
        String medicoEspecialidad,
        LocalDateTime fechaHora,
        EstadoCita estado,
        Instant fechaCancelacion,
        Instant creadoEn
) {
}

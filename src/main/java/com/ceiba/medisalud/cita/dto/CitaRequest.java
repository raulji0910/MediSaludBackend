package com.ceiba.medisalud.cita.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CitaRequest(

        @NotNull(message = "el paciente es obligatorio")
        UUID pacienteId,

        @NotNull(message = "el medico es obligatorio")
        UUID medicoId,

        @NotNull(message = "la fecha y hora son obligatorias")
        LocalDateTime fechaHora
) {
}

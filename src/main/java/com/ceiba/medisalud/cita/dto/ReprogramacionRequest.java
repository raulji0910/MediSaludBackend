package com.ceiba.medisalud.cita.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramacionRequest(

        @NotNull(message = "la nueva fecha y hora son obligatorias")
        LocalDateTime nuevaFechaHora
) {
}

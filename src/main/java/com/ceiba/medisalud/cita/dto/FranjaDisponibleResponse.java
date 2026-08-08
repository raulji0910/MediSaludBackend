package com.ceiba.medisalud.cita.dto;

import java.time.LocalDateTime;

public record FranjaDisponibleResponse(
        LocalDateTime horaInicio,
        LocalDateTime horaFin
) {
}

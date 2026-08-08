package com.ceiba.medisalud.cita.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DisponibilidadResponse(
        UUID medicoId,
        String medicoNombre,
        String medicoEspecialidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<FranjaDisponibleResponse> franjasDisponibles
) {
}

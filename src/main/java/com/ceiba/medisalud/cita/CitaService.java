package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CancelacionResponse;
import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.cita.dto.DisponibilidadResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CitaService {

    CitaResponse reservar(CitaRequest request);

    CitaResponse buscarPorId(UUID id);

    DisponibilidadResponse consultarDisponibilidad(UUID medicoId, LocalDate fechaInicio, LocalDate fechaFin);

    CancelacionResponse cancelar(UUID id);

    CitaResponse reprogramar(UUID id, LocalDateTime nuevaFechaHora);

    CitaResponse atender(UUID id);

    List<CitaResponse> listar(UUID medicoId, UUID pacienteId, EstadoCita estado,
                               LocalDate fechaInicio, LocalDate fechaFin);
}

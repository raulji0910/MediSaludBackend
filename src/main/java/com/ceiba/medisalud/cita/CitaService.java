package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;

import java.util.UUID;

public interface CitaService {

    CitaResponse reservar(CitaRequest request);

    CitaResponse buscarPorId(UUID id);
}

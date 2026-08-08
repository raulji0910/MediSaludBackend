package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;

import java.util.List;
import java.util.UUID;

public interface PacienteService {

    PacienteResponse registrar(PacienteRequest request);

    PacienteResponse buscarPorId(UUID id);

    PacienteResponse buscarPorDocumentoIdentidad(String documentoIdentidad);

    List<PacienteResponse> listar();
}

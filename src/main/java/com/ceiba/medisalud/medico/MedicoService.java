package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;

import java.util.List;
import java.util.UUID;

public interface MedicoService {

    MedicoResponse registrar(MedicoRequest request);

    MedicoResponse buscarPorId(UUID id);

    List<MedicoResponse> listar(String especialidad);
}

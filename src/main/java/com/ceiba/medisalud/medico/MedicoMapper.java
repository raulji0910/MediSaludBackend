package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;
import org.springframework.stereotype.Component;

@Component
class MedicoMapper {

    Medico toEntity(MedicoRequest request) {
        return new Medico(
                request.nombreCompleto().trim(),
                request.especialidad().trim(),
                request.telefono(),
                request.email());
    }

    MedicoResponse toResponse(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNombreCompleto(),
                medico.getEspecialidad(),
                medico.getTelefono(),
                medico.getEmail(),
                medico.getCreadoEn());
    }
}

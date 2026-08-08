package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;
import org.springframework.stereotype.Component;

@Component
class PacienteMapper {

    Paciente toEntity(PacienteRequest request) {
        return new Paciente(
                request.nombreCompleto().trim(),
                request.documentoIdentidad().trim(),
                request.telefono().trim(),
                request.email().trim(),
                request.fechaNacimiento());
    }

    PacienteResponse toResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNombreCompleto(),
                paciente.getDocumentoIdentidad(),
                paciente.getTelefono(),
                paciente.getEmail(),
                paciente.getFechaNacimiento(),
                paciente.getCreadoEn());
    }
}

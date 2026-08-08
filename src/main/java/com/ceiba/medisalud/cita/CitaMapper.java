package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CitaResponse;
import org.springframework.stereotype.Component;

@Component
class CitaMapper {

    CitaResponse toResponse(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getPaciente().getId(),
                cita.getPaciente().getNombreCompleto(),
                cita.getMedico().getId(),
                cita.getMedico().getNombreCompleto(),
                cita.getMedico().getEspecialidad(),
                cita.getFechaHora(),
                cita.getEstado(),
                cita.getFechaCancelacion(),
                cita.getCreadoEn());
    }
}

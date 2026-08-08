package com.ceiba.medisalud.cita;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CitaRepository extends JpaRepository<Cita, UUID> {

    boolean existsByMedicoIdAndFechaHoraAndEstado(UUID medicoId, LocalDateTime fechaHora, EstadoCita estado);

    boolean existsByPacienteIdAndFechaHoraAndEstado(UUID pacienteId, LocalDateTime fechaHora, EstadoCita estado);
}

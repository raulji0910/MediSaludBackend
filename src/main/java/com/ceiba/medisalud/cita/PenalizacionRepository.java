package com.ceiba.medisalud.cita;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {

    long countByPacienteIdAndFechaPenalizacionAfter(UUID pacienteId, Instant desde);
}

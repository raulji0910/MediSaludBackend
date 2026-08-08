package com.ceiba.medisalud.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

    Optional<Paciente> findByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);
}

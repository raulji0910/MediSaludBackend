package com.ceiba.medisalud.medico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {

    List<Medico> findByEspecialidadIgnoreCase(String especialidad);
}

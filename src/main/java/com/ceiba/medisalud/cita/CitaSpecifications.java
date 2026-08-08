package com.ceiba.medisalud.cita;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

final class CitaSpecifications {

    private CitaSpecifications() {
    }

    static Specification<Cita> conMedicoId(UUID medicoId) {
        return (root, query, cb) -> medicoId == null ? null : cb.equal(root.get("medico").get("id"), medicoId);
    }

    static Specification<Cita> conPacienteId(UUID pacienteId) {
        return (root, query, cb) -> pacienteId == null ? null : cb.equal(root.get("paciente").get("id"), pacienteId);
    }

    static Specification<Cita> conEstado(EstadoCita estado) {
        return (root, query, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    static Specification<Cita> conFechaHoraDesde(LocalDateTime desde) {
        return (root, query, cb) -> desde == null ? null : cb.greaterThanOrEqualTo(root.get("fechaHora"), desde);
    }

    static Specification<Cita> conFechaHoraHasta(LocalDateTime hasta) {
        return (root, query, cb) -> hasta == null ? null : cb.lessThan(root.get("fechaHora"), hasta);
    }
}

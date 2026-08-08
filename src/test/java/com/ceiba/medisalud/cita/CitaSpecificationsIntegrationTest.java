package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.medico.Medico;
import com.ceiba.medisalud.medico.MedicoRepository;
import com.ceiba.medisalud.paciente.Paciente;
import com.ceiba.medisalud.paciente.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class CitaSpecificationsIntegrationTest {

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;

    private Medico medico1;
    private Medico medico2;
    private Paciente paciente1;
    private Paciente paciente2;

    @BeforeEach
    void setUp() {
        List<Medico> medicos = medicoRepository.findAll();
        medico1 = medicos.get(0);
        medico2 = medicos.get(1);

        paciente1 = pacienteRepository.save(new Paciente("Filtro Uno", "F0000001", "3000000001", "f1@mail.com", null));
        paciente2 = pacienteRepository.save(new Paciente("Filtro Dos", "F0000002", "3000000002", "f2@mail.com", null));

        Cita c1 = new Cita(paciente1, medico1, LocalDateTime.of(2027, 3, 1, 9, 0));
        Cita c2 = new Cita(paciente2, medico1, LocalDateTime.of(2027, 3, 1, 10, 0));
        Cita c3 = new Cita(paciente1, medico2, LocalDateTime.of(2027, 3, 5, 9, 0));
        c3.setEstado(EstadoCita.CANCELADA);
        Cita c4 = new Cita(paciente2, medico2, LocalDateTime.of(2027, 3, 10, 9, 0));

        citaRepository.saveAll(List.of(c1, c2, c3, c4));
    }

    @Test
    void filtrarPorMedicoId_debeRetornarSoloLasCitasDeEseMedico() {
        Specification<Cita> filtro = Specification.where(CitaSpecifications.conMedicoId(medico1.getId()));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(2)
                .allMatch(c -> c.getMedico().getId().equals(medico1.getId()));
    }

    @Test
    void filtrarPorPacienteId_debeRetornarSoloLasCitasDeEsePaciente() {
        Specification<Cita> filtro = Specification.where(CitaSpecifications.conPacienteId(paciente1.getId()));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(2)
                .allMatch(c -> c.getPaciente().getId().equals(paciente1.getId()));
    }

    @Test
    void filtrarPorEstado_debeRetornarSoloLasCitasConEseEstado() {
        Specification<Cita> filtro = Specification.where(CitaSpecifications.conEstado(EstadoCita.CANCELADA));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void filtrarPorRangoDeFechas_debeRetornarSoloLasCitasEnEseRango() {
        Specification<Cita> filtro = Specification
                .where(CitaSpecifications.conFechaHoraDesde(LocalDateTime.of(2027, 3, 1, 0, 0)))
                .and(CitaSpecifications.conFechaHoraHasta(LocalDateTime.of(2027, 3, 6, 0, 0)));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(3);
    }

    @Test
    void combinarFiltros_debeAplicarTodosLosFiltrosALaVez() {
        Specification<Cita> filtro = Specification.where(CitaSpecifications.conMedicoId(medico1.getId()))
                .and(CitaSpecifications.conPacienteId(paciente1.getId()));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMedico().getId()).isEqualTo(medico1.getId());
        assertThat(resultado.get(0).getPaciente().getId()).isEqualTo(paciente1.getId());
    }

    @Test
    void sinFiltros_debeRetornarTodasLasCitas() {
        Specification<Cita> filtro = Specification.where(CitaSpecifications.conPacienteId(null))
                .and(CitaSpecifications.conMedicoId(null))
                .and(CitaSpecifications.conEstado(null));

        List<Cita> resultado = citaRepository.findAll(filtro);

        assertThat(resultado).hasSize(4);
    }
}

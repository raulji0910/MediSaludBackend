package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.medico.Medico;
import com.ceiba.medisalud.medico.MedicoRepository;
import com.ceiba.medisalud.paciente.Paciente;
import com.ceiba.medisalud.paciente.PacienteRepository;
import com.ceiba.medisalud.shared.exception.BusinessRuleException;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private PenalizacionRepository penalizacionRepository;

    private CitaService citaService;

    private Paciente paciente;
    private Medico medico;

    @BeforeEach
    void setUp() {
        HorarioAtencionPolicy horarioAtencionPolicy = new HorarioAtencionPolicy(new SinFestivosConfiguradosPolicy());
        citaService = new CitaServiceImpl(citaRepository, pacienteRepository, medicoRepository,
                penalizacionRepository, horarioAtencionPolicy, new CitaMapper());

        paciente = new Paciente("Juan Perez", "1002003004", "3001234567", "juan.perez@mail.com", null);
        medico = new Medico("Dra. Maria Gonzalez", "Cardiologia", "555-1001", "maria.gonzalez@medisalud.com");
    }

    private LocalDateTime proximoLunesValido() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .with(LocalTime.of(9, 0));
    }

    private LocalDateTime proximoDomingo() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .with(LocalTime.of(9, 0));
    }

    @Test
    void reservar_debeCrearLaCita_cuandoTodoEsValido() {
        LocalDateTime fechaHora = proximoLunesValido();
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), fechaHora);
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(eq(paciente.getId()), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaHora, EstadoCita.PROGRAMADA)).thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaHora, EstadoCita.PROGRAMADA)).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaResponse response = citaService.reservar(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(response.pacienteId()).isEqualTo(paciente.getId());
        assertThat(response.medicoId()).isEqualTo(medico.getId());
        assertThat(response.fechaHora()).isEqualTo(fechaHora);
    }

    @Test
    void reservar_debeLanzarResourceNotFoundException_cuandoElPacienteNoExiste() {
        UUID pacienteIdInexistente = UUID.randomUUID();
        CitaRequest request = new CitaRequest(pacienteIdInexistente, medico.getId(), proximoLunesValido());
        when(pacienteRepository.findById(pacienteIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reservar_debeLanzarResourceNotFoundException_cuandoElMedicoNoExiste() {
        UUID medicoIdInexistente = UUID.randomUUID();
        CitaRequest request = new CitaRequest(paciente.getId(), medicoIdInexistente, proximoLunesValido());
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medicoIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reservar_debeLanzarBusinessRuleException_cuandoLaFechaYaPaso() {
        LocalDateTime fechaPasada = LocalDateTime.now().minusDays(1);
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), fechaPasada);
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reservar_debeLanzarBusinessRuleException_cuandoLaFranjaNoEsValida_porqueEsDomingo() {
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), proximoDomingo());
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reservar_debeLanzarConflictException_cuandoElMedicoYaTieneCitaEnEsaFranja() {
        LocalDateTime fechaHora = proximoLunesValido();
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), fechaHora);
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(eq(paciente.getId()), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaHora, EstadoCita.PROGRAMADA)).thenReturn(true);

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reservar_debeLanzarConflictException_cuandoElPacienteYaTieneCitaEnEsaFranjaConOtroMedico() {
        LocalDateTime fechaHora = proximoLunesValido();
        Medico otroMedico = new Medico("Dr. Carlos Ruiz", "Pediatria", "555-1002", "carlos.ruiz@medisalud.com");
        CitaRequest request = new CitaRequest(paciente.getId(), otroMedico.getId(), fechaHora);
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(otroMedico.getId())).thenReturn(Optional.of(otroMedico));
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(eq(paciente.getId()), any())).thenReturn(0L);
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(otroMedico.getId(), fechaHora, EstadoCita.PROGRAMADA)).thenReturn(false);
        // El paciente ya tiene una cita en esa franja, aunque sea con un medico distinto (RN-04, conflicto global)
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaHora, EstadoCita.PROGRAMADA)).thenReturn(true);

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reservar_debeLanzarBusinessRuleException_cuandoElPacienteTiene3OMasPenalizacionesEnLosUltimos30Dias() {
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), proximoLunesValido());
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));
        when(penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(eq(paciente.getId()), any())).thenReturn(3L);

        assertThatThrownBy(() -> citaService.reservar(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void buscarPorId_debeLanzarResourceNotFoundException_cuandoNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(citaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.buscarPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CancelacionResponse;
import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.cita.dto.DisponibilidadResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        CitaProperties citaProperties = HorarioAtencionPolicyTest.propiedadesPorDefecto();
        HorarioAtencionPolicy horarioAtencionPolicy =
                new HorarioAtencionPolicy(new SinFestivosConfiguradosPolicy(), citaProperties);
        citaService = new CitaServiceImpl(citaRepository, pacienteRepository, medicoRepository,
                penalizacionRepository, horarioAtencionPolicy, new CitaMapper(), citaProperties);

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
    void reservar_debeLanzarBusinessRuleException_cuandoLaFechaDeNacimientoDelPacienteEsFutura() {
        // RN-03: el registro de pacientes ya bloquea fechas de nacimiento futuras (@PastOrPresent),
        // asi que esta ruta solo se ejercita si la entidad llega con el dato en un estado invalido
        // por fuera del flujo normal de la API. Se prueba igual porque la validacion existe en el
        // codigo y debe quedar cubierta, no solo asumida como "nunca va a pasar".
        paciente.setFechaNacimiento(LocalDate.now().plusDays(1));
        CitaRequest request = new CitaRequest(paciente.getId(), medico.getId(), proximoLunesValido());
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
    void consultarDisponibilidad_debeLanzarResourceNotFoundException_cuandoElMedicoNoExiste() {
        UUID medicoIdInexistente = UUID.randomUUID();
        LocalDate fecha = proximoLunesValido().toLocalDate();
        when(medicoRepository.findById(medicoIdInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.consultarDisponibilidad(medicoIdInexistente, fecha, fecha))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void consultarDisponibilidad_debeLanzarBusinessRuleException_cuandoFechaFinEsAnteriorAFechaInicio() {
        LocalDate fecha = proximoLunesValido().toLocalDate();
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> citaService.consultarDisponibilidad(medico.getId(), fecha, fecha.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void consultarDisponibilidad_debeExcluirLaFranjaYaOcupadaPorUnaCita() {
        LocalDate lunes = proximoLunesValido().toLocalDate();
        LocalDateTime franjaOcupada = LocalDateTime.of(lunes, LocalTime.of(9, 0));
        Cita citaExistente = new Cita(paciente, medico, franjaOcupada);
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));
        when(citaRepository.findByMedicoIdAndEstadoAndFechaHoraBetween(eq(medico.getId()), eq(EstadoCita.PROGRAMADA),
                any(), any())).thenReturn(List.of(citaExistente));

        DisponibilidadResponse response = citaService.consultarDisponibilidad(medico.getId(), lunes, lunes);

        assertThat(response.franjasDisponibles())
                .extracting(f -> f.horaInicio())
                .doesNotContain(franjaOcupada)
                .contains(franjaOcupada.plusMinutes(30));
    }

    @Test
    void consultarDisponibilidad_debeRetornar110Franjas_enUnaSemanaCompletaSinCitasOcupadas() {
        LocalDate lunes = proximoLunesValido().toLocalDate();
        LocalDate domingo = lunes.plusDays(6);
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));
        when(citaRepository.findByMedicoIdAndEstadoAndFechaHoraBetween(eq(medico.getId()), eq(EstadoCita.PROGRAMADA),
                any(), any())).thenReturn(List.of());

        DisponibilidadResponse response = citaService.consultarDisponibilidad(medico.getId(), lunes, domingo);

        assertThat(response.franjasDisponibles()).hasSize(110);
        assertThat(response.franjasDisponibles())
                .extracting(f -> f.horaInicio().toLocalDate().getDayOfWeek())
                .doesNotContain(DayOfWeek.SUNDAY);
    }

    @Test
    void cancelar_debeCambiarEstadoACancelada_yNoRegistrarPenalizacion_cuandoHayMasDe2HorasDeAntelacion() {
        LocalDateTime fechaHora = LocalDateTime.now().plusDays(3);
        Cita cita = new Cita(paciente, medico, fechaHora);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelacionResponse response = citaService.cancelar(cita.getId());

        assertThat(response.estado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(response.fechaCancelacion()).isNotNull();
        assertThat(response.penalizacionRegistrada()).isFalse();
        verify(penalizacionRepository, never()).save(any());
    }

    @Test
    void cancelar_debeRegistrarPenalizacion_cuandoHayMenosDe2HorasDeAntelacion() {
        LocalDateTime fechaHora = LocalDateTime.now().plusHours(1);
        Cita cita = new Cita(paciente, medico, fechaHora);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelacionResponse response = citaService.cancelar(cita.getId());

        assertThat(response.penalizacionRegistrada()).isTrue();
        verify(penalizacionRepository).save(any());
    }

    @Test
    void cancelar_debeRegistrarPenalizacion_cuandoLaCitaYaPaso() {
        LocalDateTime fechaHora = LocalDateTime.now().minusHours(1);
        Cita cita = new Cita(paciente, medico, fechaHora);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelacionResponse response = citaService.cancelar(cita.getId());

        assertThat(response.penalizacionRegistrada()).isTrue();
    }

    @Test
    void cancelar_debeLanzarResourceNotFoundException_cuandoLaCitaNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(citaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.cancelar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelar_debeLanzarConflictException_cuandoLaCitaYaEstaCancelada() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.cancelar(cita.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reprogramar_debeCancelarLaAnterior_yCrearUnaNuevaConElNuevoHorario() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(3);
        LocalDateTime fechaNueva = proximoLunesValido();
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaResponse response = citaService.reprogramar(citaOriginal.getId(), fechaNueva);

        assertThat(citaOriginal.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(response.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(response.fechaHora()).isEqualTo(fechaNueva);
        assertThat(response.id()).isNotEqualTo(citaOriginal.getId());
    }

    @Test
    void reprogramar_debeLanzarResourceNotFoundException_cuandoLaCitaNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(citaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.reprogramar(idInexistente, proximoLunesValido()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reprogramar_debeLanzarConflictException_cuandoLaCitaActualNoEstaProgramada() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.reprogramar(cita.getId(), proximoLunesValido()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reprogramar_debeRegistrarPenalizacion_cuandoLaCancelacionDeLaAnteriorEsTardia() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusHours(1);
        LocalDateTime fechaNueva = proximoLunesValido();
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        citaService.reprogramar(citaOriginal.getId(), fechaNueva);

        verify(penalizacionRepository).save(any());
    }

    @Test
    void reprogramar_debeLanzarConflictException_cuandoElNuevoHorarioYaEstaOcupadoPorElMedico() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(3);
        LocalDateTime fechaNueva = proximoLunesValido();
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(true);

        assertThatThrownBy(() -> citaService.reprogramar(citaOriginal.getId(), fechaNueva))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reprogramar_debeLanzarConflictException_cuandoElNuevoHorarioYaLoTieneElPacienteConOtroMedico() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(3);
        LocalDateTime fechaNueva = proximoLunesValido();
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(true);

        assertThatThrownBy(() -> citaService.reprogramar(citaOriginal.getId(), fechaNueva))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reprogramar_debeLanzarBusinessRuleException_cuandoElNuevoHorarioNoEsValido() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(3);
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));

        assertThatThrownBy(() -> citaService.reprogramar(citaOriginal.getId(), proximoDomingo()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reprogramar_noDebeAplicarElBloqueoPorPenalizaciones_aunqueElPacienteTenga3OMas() {
        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(3);
        LocalDateTime fechaNueva = proximoLunesValido();
        Cita citaOriginal = new Cita(paciente, medico, fechaOriginal);
        when(citaRepository.findById(citaOriginal.getId())).thenReturn(Optional.of(citaOriginal));
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medico.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.existsByPacienteIdAndFechaHoraAndEstado(paciente.getId(), fechaNueva, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaResponse response = citaService.reprogramar(citaOriginal.getId(), fechaNueva);

        assertThat(response.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        verify(penalizacionRepository, never()).countByPacienteIdAndFechaPenalizacionAfter(any(), any());
    }

    @Test
    void atender_debeCambiarEstadoAAtendida_cuandoLaCitaEstaProgramada() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitaResponse response = citaService.atender(cita.getId());

        assertThat(response.estado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void atender_debeLanzarResourceNotFoundException_cuandoLaCitaNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(citaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.atender(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atender_debeLanzarConflictException_cuandoLaCitaYaFueCancelada() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.atender(cita.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void atender_debeLanzarConflictException_cuandoLaCitaYaFueAtendida() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        cita.setEstado(EstadoCita.ATENDIDA);
        when(citaRepository.findById(cita.getId())).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.atender(cita.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listar_debeLanzarBusinessRuleException_cuandoFechaFinEsAnteriorAFechaInicio() {
        LocalDate fecha = LocalDate.now();

        assertThatThrownBy(() -> citaService.listar(null, null, null, fecha, fecha.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listar_debeMapearLasCitasEncontradasPorElRepositorio() {
        Cita cita = new Cita(paciente, medico, LocalDateTime.now().plusDays(1));
        when(citaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(cita));

        List<CitaResponse> resultado = citaService.listar(medico.getId(), null, null, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).medicoId()).isEqualTo(medico.getId());
    }

    @Test
    void buscarPorId_debeLanzarResourceNotFoundException_cuandoNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(citaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citaService.buscarPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

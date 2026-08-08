package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CancelacionResponse;
import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.cita.dto.DisponibilidadResponse;
import com.ceiba.medisalud.cita.dto.FranjaDisponibleResponse;
import com.ceiba.medisalud.medico.Medico;
import com.ceiba.medisalud.medico.MedicoRepository;
import com.ceiba.medisalud.paciente.Paciente;
import com.ceiba.medisalud.paciente.PacienteRepository;
import com.ceiba.medisalud.shared.exception.BusinessRuleException;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final HorarioAtencionPolicy horarioAtencionPolicy;
    private final CitaMapper citaMapper;
    private final CitaProperties citaProperties;

    CitaServiceImpl(CitaRepository citaRepository, PacienteRepository pacienteRepository,
                     MedicoRepository medicoRepository, PenalizacionRepository penalizacionRepository,
                     HorarioAtencionPolicy horarioAtencionPolicy, CitaMapper citaMapper,
                     CitaProperties citaProperties) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.horarioAtencionPolicy = horarioAtencionPolicy;
        this.citaMapper = citaMapper;
        this.citaProperties = citaProperties;
    }

    @Override
    public CitaResponse reservar(CitaRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un paciente con id " + request.pacienteId()));
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un medico con id " + request.medicoId()));

        LocalDateTime fechaHora = request.fechaHora();
        validarFechaHoraNoPasada(fechaHora);
        validarFranjaHoraria(fechaHora);
        validarEdadMinima(paciente);
        validarSinPenalizacionesActivas(paciente);
        validarDisponibilidadMedico(medico.getId(), fechaHora);
        validarDisponibilidadPaciente(paciente.getId(), fechaHora);

        Cita cita = new Cita(paciente, medico, fechaHora);
        Cita guardada = citaRepository.save(cita);
        return citaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public CitaResponse buscarPorId(UUID id) {
        return citaMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(UUID medicoId, LocalDate fechaInicio, LocalDate fechaFin) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un medico con id " + medicoId));
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessRuleException("La fecha fin no puede ser anterior a la fecha inicio");
        }

        Set<LocalDateTime> franjasOcupadas = citaRepository
                .findByMedicoIdAndEstadoAndFechaHoraBetween(medicoId, EstadoCita.PROGRAMADA,
                        fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay())
                .stream()
                .map(Cita::getFechaHora)
                .collect(Collectors.toSet());

        LocalDateTime ahora = LocalDateTime.now();
        List<FranjaDisponibleResponse> franjasDisponibles = new ArrayList<>();
        for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
            for (LocalTime hora : horarioAtencionPolicy.franjasDelDia(fecha)) {
                LocalDateTime inicio = LocalDateTime.of(fecha, hora);
                if (!inicio.isBefore(ahora) && !franjasOcupadas.contains(inicio)) {
                    franjasDisponibles.add(new FranjaDisponibleResponse(inicio,
                            inicio.plusMinutes(citaProperties.franjaMinutos())));
                }
            }
        }

        return new DisponibilidadResponse(medico.getId(), medico.getNombreCompleto(), medico.getEspecialidad(),
                fechaInicio, fechaFin, franjasDisponibles);
    }

    @Override
    public CancelacionResponse cancelar(UUID id) {
        Cita cita = obtenerEntidadPorId(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictException("Solo se pueden cancelar citas en estado PROGRAMADA");
        }

        boolean penalizacionRegistrada = cancelarInternamente(cita);

        return new CancelacionResponse(cita.getId(), cita.getEstado(), cita.getFechaCancelacion(),
                penalizacionRegistrada);
    }

    @Override
    public CitaResponse reprogramar(UUID id, LocalDateTime nuevaFechaHora) {
        Cita citaActual = obtenerEntidadPorId(id);
        if (citaActual.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictException("Solo se pueden reprogramar citas en estado PROGRAMADA");
        }

        Paciente paciente = citaActual.getPaciente();
        Medico medico = citaActual.getMedico();

        // Paso 1 (RN-06): cancelar la cita anterior, aplicando RN-05 si corresponde
        cancelarInternamente(citaActual);

        // Paso 2 y 3 (RN-06): crear la nueva cita, validando que el nuevo horario este disponible.
        // El enunciado solo pide validar RN-02/RN-04 aqui; RN-01 (franja valida) y RN-03 (edad) se
        // mantienen porque son invariantes estructurales de cualquier cita, no reglas de agendamiento
        // que tenga sentido saltarse. Deliberadamente NO se vuelve a aplicar el bloqueo por 3+
        // penalizaciones (RN-05 parte 1): el enunciado solo menciona RN-02 y RN-04 para este paso, y
        // reprogramar no es un agendamiento nuevo sino mover uno ya existente.
        validarFechaHoraNoPasada(nuevaFechaHora);
        validarFranjaHoraria(nuevaFechaHora);
        validarEdadMinima(paciente);
        validarDisponibilidadMedico(medico.getId(), nuevaFechaHora);
        validarDisponibilidadPaciente(paciente.getId(), nuevaFechaHora);

        Cita nuevaCita = new Cita(paciente, medico, nuevaFechaHora);
        Cita guardada = citaRepository.save(nuevaCita);
        return citaMapper.toResponse(guardada);
    }

    private boolean cancelarInternamente(Cita cita) {
        boolean penalizacionRegistrada = esCancelacionTardia(cita.getFechaHora());
        if (penalizacionRegistrada) {
            Penalizacion penalizacion = new Penalizacion(cita.getPaciente(), cita.getId(),
                    "Cancelacion con menos de " + citaProperties.penalizacion().horasMinimasCancelacion()
                            + " horas de antelacion");
            penalizacionRepository.save(penalizacion);
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setFechaCancelacion(Instant.now());
        citaRepository.save(cita);
        return penalizacionRegistrada;
    }

    @Override
    public CitaResponse atender(UUID id) {
        Cita cita = obtenerEntidadPorId(id);
        if (cita.getEstado() != EstadoCita.PROGRAMADA) {
            throw new ConflictException("Solo se pueden marcar como atendidas citas en estado PROGRAMADA");
        }

        cita.setEstado(EstadoCita.ATENDIDA);
        Cita guardada = citaRepository.save(cita);
        return citaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listar(UUID medicoId, UUID pacienteId, EstadoCita estado,
                                      LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new BusinessRuleException("La fecha fin no puede ser anterior a la fecha inicio");
        }

        LocalDateTime desde = fechaInicio == null ? null : fechaInicio.atStartOfDay();
        LocalDateTime hasta = fechaFin == null ? null : fechaFin.plusDays(1).atStartOfDay();

        Specification<Cita> filtro = Specification.where(CitaSpecifications.conMedicoId(medicoId))
                .and(CitaSpecifications.conPacienteId(pacienteId))
                .and(CitaSpecifications.conEstado(estado))
                .and(CitaSpecifications.conFechaHoraDesde(desde))
                .and(CitaSpecifications.conFechaHoraHasta(hasta));

        return citaRepository.findAll(filtro, Sort.by(Sort.Direction.ASC, "fechaHora")).stream()
                .map(citaMapper::toResponse)
                .toList();
    }

    private boolean esCancelacionTardia(LocalDateTime fechaHoraCita) {
        Duration antelacion = Duration.between(LocalDateTime.now(), fechaHoraCita);
        return antelacion.toMinutes() < citaProperties.penalizacion().horasMinimasCancelacion() * 60L;
    }

    Cita obtenerEntidadPorId(UUID id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cita con id " + id));
    }

    private void validarFechaHoraNoPasada(LocalDateTime fechaHora) {
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("No se pueden agendar citas en una fecha u hora que ya paso");
        }
    }

    private void validarFranjaHoraria(LocalDateTime fechaHora) {
        if (!horarioAtencionPolicy.esFranjaValida(fechaHora)) {
            CitaProperties.Horario horario = citaProperties.horario();
            throw new BusinessRuleException("La fecha y hora debe corresponder a una franja de "
                    + citaProperties.franjaMinutos() + " minutos dentro del horario de atencion "
                    + "(lunes a viernes " + horario.apertura() + "-" + horario.cierreEntreSemana()
                    + ", sabados " + horario.apertura() + "-" + horario.cierreSabado() + ")");
        }
    }

    private void validarEdadMinima(Paciente paciente) {
        LocalDate fechaNacimiento = paciente.getFechaNacimiento();
        if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de nacimiento del paciente no puede ser futura");
        }
    }

    private void validarSinPenalizacionesActivas(Paciente paciente) {
        int diasVentana = citaProperties.penalizacion().diasVentana();
        int maxPermitidas = citaProperties.penalizacion().maxPermitidas();
        Instant desde = Instant.now().minus(diasVentana, ChronoUnit.DAYS);
        long penalizaciones = penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(paciente.getId(), desde);
        if (penalizaciones >= maxPermitidas) {
            throw new BusinessRuleException("El paciente tiene " + penalizaciones + " penalizaciones en los "
                    + "ultimos " + diasVentana + " dias y no puede agendar nuevas citas");
        }
    }

    private void validarDisponibilidadMedico(UUID medicoId, LocalDateTime fechaHora) {
        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, fechaHora, EstadoCita.PROGRAMADA)) {
            throw new ConflictException("El medico ya tiene una cita programada en esa franja horaria");
        }
    }

    private void validarDisponibilidadPaciente(UUID pacienteId, LocalDateTime fechaHora) {
        if (citaRepository.existsByPacienteIdAndFechaHoraAndEstado(pacienteId, fechaHora, EstadoCita.PROGRAMADA)) {
            throw new ConflictException("El paciente ya tiene una cita programada en esa franja horaria");
        }
    }
}

package com.ceiba.medisalud.cita;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int DIAS_VENTANA_PENALIZACION = 30;
    private static final int MAX_PENALIZACIONES_PERMITIDAS = 3;

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PenalizacionRepository penalizacionRepository;
    private final HorarioAtencionPolicy horarioAtencionPolicy;
    private final CitaMapper citaMapper;

    CitaServiceImpl(CitaRepository citaRepository, PacienteRepository pacienteRepository,
                     MedicoRepository medicoRepository, PenalizacionRepository penalizacionRepository,
                     HorarioAtencionPolicy horarioAtencionPolicy, CitaMapper citaMapper) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.penalizacionRepository = penalizacionRepository;
        this.horarioAtencionPolicy = horarioAtencionPolicy;
        this.citaMapper = citaMapper;
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
                            inicio.plusMinutes(HorarioAtencionPolicy.DURACION_FRANJA_MINUTOS)));
                }
            }
        }

        return new DisponibilidadResponse(medico.getId(), medico.getNombreCompleto(), medico.getEspecialidad(),
                fechaInicio, fechaFin, franjasDisponibles);
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
            throw new BusinessRuleException("La fecha y hora debe corresponder a una franja de 30 minutos dentro "
                    + "del horario de atencion (lunes a viernes 08:00-18:00, sabados 08:00-13:00)");
        }
    }

    private void validarEdadMinima(Paciente paciente) {
        LocalDate fechaNacimiento = paciente.getFechaNacimiento();
        if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de nacimiento del paciente no puede ser futura");
        }
    }

    private void validarSinPenalizacionesActivas(Paciente paciente) {
        Instant desde = Instant.now().minus(DIAS_VENTANA_PENALIZACION, ChronoUnit.DAYS);
        long penalizaciones = penalizacionRepository.countByPacienteIdAndFechaPenalizacionAfter(paciente.getId(), desde);
        if (penalizaciones >= MAX_PENALIZACIONES_PERMITIDAS) {
            throw new BusinessRuleException("El paciente tiene " + penalizaciones + " penalizaciones en los "
                    + "ultimos " + DIAS_VENTANA_PENALIZACION + " dias y no puede agendar nuevas citas");
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

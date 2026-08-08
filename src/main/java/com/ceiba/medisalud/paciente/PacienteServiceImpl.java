package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PacienteMapper pacienteMapper;

    PacienteServiceImpl(PacienteRepository pacienteRepository, PacienteMapper pacienteMapper) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteMapper = pacienteMapper;
    }

    @Override
    public PacienteResponse registrar(PacienteRequest request) {
        String documentoIdentidad = request.documentoIdentidad().trim();
        if (pacienteRepository.existsByDocumentoIdentidad(documentoIdentidad)) {
            throw new ConflictException("Ya existe un paciente registrado con el documento " + documentoIdentidad);
        }

        Paciente paciente = pacienteMapper.toEntity(request);
        try {
            Paciente guardado = pacienteRepository.save(paciente);
            return pacienteMapper.toResponse(guardado);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Ya existe un paciente registrado con el documento " + documentoIdentidad);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(UUID id) {
        return pacienteMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResponse buscarPorDocumentoIdentidad(String documentoIdentidad) {
        return pacienteRepository.findByDocumentoIdentidad(documentoIdentidad.trim())
                .map(pacienteMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un paciente con documento de identidad " + documentoIdentidad));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return pacienteRepository.findAll().stream()
                .map(pacienteMapper::toResponse)
                .toList();
    }

    Paciente obtenerEntidadPorId(UUID id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un paciente con id " + id));
    }
}

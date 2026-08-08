package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
class MedicoServiceImpl implements MedicoService {

    private final MedicoRepository medicoRepository;
    private final MedicoMapper medicoMapper;

    MedicoServiceImpl(MedicoRepository medicoRepository, MedicoMapper medicoMapper) {
        this.medicoRepository = medicoRepository;
        this.medicoMapper = medicoMapper;
    }

    @Override
    public MedicoResponse registrar(MedicoRequest request) {
        Medico medico = medicoMapper.toEntity(request);
        Medico guardado = medicoRepository.save(medico);
        return medicoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicoResponse buscarPorId(UUID id) {
        return medicoMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicoResponse> listar(String especialidad) {
        List<Medico> medicos = especialidad == null || especialidad.isBlank()
                ? medicoRepository.findAll()
                : medicoRepository.findByEspecialidadIgnoreCase(especialidad.trim());

        return medicos.stream()
                .map(medicoMapper::toResponse)
                .toList();
    }

    Medico obtenerEntidadPorId(UUID id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un medico con id " + id));
    }
}

package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceImplTest {

    @Mock
    private PacienteRepository pacienteRepository;

    private PacienteService pacienteService;

    @BeforeEach
    void setUp() {
        pacienteService = new PacienteServiceImpl(pacienteRepository, new PacienteMapper());
    }

    @Test
    void registrar_debeGuardarYRetornarElPacienteCreado() {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", LocalDate.of(1990, 5, 20));
        when(pacienteRepository.existsByDocumentoIdentidad("1002003004")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PacienteResponse response = pacienteService.registrar(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.documentoIdentidad()).isEqualTo("1002003004");
        assertThat(response.fechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 20));
    }

    @Test
    void registrar_debeLanzarConflictException_cuandoElDocumentoYaExiste() {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", null);
        when(pacienteRepository.existsByDocumentoIdentidad("1002003004")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.registrar(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("1002003004");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void registrar_debeLanzarConflictException_cuandoLaBaseDeDatosRechazaPorDocumentoDuplicado() {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", null);
        when(pacienteRepository.existsByDocumentoIdentidad("1002003004")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> pacienteService.registrar(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void buscarPorId_debeRetornarElPaciente_cuandoExiste() {
        Paciente paciente = new Paciente("Juan Perez", "1002003004", "3001234567", "juan.perez@mail.com", null);
        when(pacienteRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));

        PacienteResponse response = pacienteService.buscarPorId(paciente.getId());

        assertThat(response.id()).isEqualTo(paciente.getId());
    }

    @Test
    void buscarPorId_debeLanzarResourceNotFoundException_cuandoNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(pacienteRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarPorDocumentoIdentidad_debeRetornarElPaciente_cuandoExiste() {
        Paciente paciente = new Paciente("Juan Perez", "1002003004", "3001234567", "juan.perez@mail.com", null);
        when(pacienteRepository.findByDocumentoIdentidad("1002003004")).thenReturn(Optional.of(paciente));

        PacienteResponse response = pacienteService.buscarPorDocumentoIdentidad("1002003004");

        assertThat(response.documentoIdentidad()).isEqualTo("1002003004");
    }

    @Test
    void buscarPorDocumentoIdentidad_debeLanzarResourceNotFoundException_cuandoNoExiste() {
        when(pacienteRepository.findByDocumentoIdentidad("9999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorDocumentoIdentidad("9999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999999");
    }

    @Test
    void listar_debeRetornarTodosLosPacientes() {
        Paciente paciente = new Paciente("Juan Perez", "1002003004", "3001234567", "juan.perez@mail.com", null);
        when(pacienteRepository.findAll()).thenReturn(List.of(paciente));

        List<PacienteResponse> resultado = pacienteService.listar();

        assertThat(resultado).hasSize(1);
    }
}

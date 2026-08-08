package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicoServiceImplTest {

    @Mock
    private MedicoRepository medicoRepository;

    private MedicoService medicoService;

    @BeforeEach
    void setUp() {
        medicoService = new MedicoServiceImpl(medicoRepository, new MedicoMapper());
    }

    @Test
    void registrar_debeGuardarYRetornarElMedicoCreado() {
        MedicoRequest request = new MedicoRequest("Dra. Maria Gonzalez", "Cardiologia", "555-1001",
                "maria.gonzalez@medisalud.com");
        when(medicoRepository.save(any(Medico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicoResponse response = medicoService.registrar(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.nombreCompleto()).isEqualTo("Dra. Maria Gonzalez");
        assertThat(response.especialidad()).isEqualTo("Cardiologia");
        assertThat(response.telefono()).isEqualTo("555-1001");
        assertThat(response.email()).isEqualTo("maria.gonzalez@medisalud.com");
        verify(medicoRepository).save(any(Medico.class));
    }

    @Test
    void buscarPorId_debeRetornarElMedico_cuandoExiste() {
        Medico medico = new Medico("Dr. Carlos Ruiz", "Pediatria", "555-1002", "carlos.ruiz@medisalud.com");
        when(medicoRepository.findById(medico.getId())).thenReturn(Optional.of(medico));

        MedicoResponse response = medicoService.buscarPorId(medico.getId());

        assertThat(response.id()).isEqualTo(medico.getId());
        assertThat(response.nombreCompleto()).isEqualTo("Dr. Carlos Ruiz");
    }

    @Test
    void buscarPorId_debeLanzarResourceNotFoundException_cuandoNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(medicoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.buscarPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(idInexistente.toString());
    }

    @Test
    void listar_debeRetornarTodosLosMedicos_cuandoNoSeEnviaFiltro() {
        Medico medico = new Medico("Dra. Ana Lopez", "Dermatologia", null, null);
        when(medicoRepository.findAll()).thenReturn(List.of(medico));

        List<MedicoResponse> resultado = medicoService.listar(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreCompleto()).isEqualTo("Dra. Ana Lopez");
    }

    @Test
    void listar_debeFiltrarPorEspecialidad_cuandoSeEnviaFiltro() {
        Medico medico = new Medico("Dra. Ana Lopez", "Dermatologia", null, null);
        when(medicoRepository.findByEspecialidadIgnoreCase("Dermatologia")).thenReturn(List.of(medico));

        List<MedicoResponse> resultado = medicoService.listar("Dermatologia");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).especialidad()).isEqualTo("Dermatologia");
    }
}

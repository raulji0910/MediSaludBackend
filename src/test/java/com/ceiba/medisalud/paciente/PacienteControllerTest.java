package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PacienteService pacienteService;

    @Test
    void registrar_debeRetornar201_cuandoElRequestEsValido() throws Exception {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", null);
        PacienteResponse response = new PacienteResponse(UUID.randomUUID(), "Juan Perez", "1002003004",
                "3001234567", "juan.perez@mail.com", null, Instant.now());
        when(pacienteService.registrar(any(PacienteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pacientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentoIdentidad").value("1002003004"));
    }

    @Test
    void registrar_debeRetornar400_cuandoElDocumentoEsMuyCorto() throws Exception {
        PacienteRequest request = new PacienteRequest("Juan Perez", "123", "3001234567",
                "juan.perez@mail.com", null);

        mockMvc.perform(post("/api/pacientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("documentoIdentidad"));
    }

    @Test
    void registrar_debeRetornar400_cuandoElEmailEsInvalido() throws Exception {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "no-es-un-email", null);

        mockMvc.perform(post("/api/pacientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_debeRetornar409_cuandoElDocumentoYaExiste() throws Exception {
        PacienteRequest request = new PacienteRequest("Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", null);
        when(pacienteService.registrar(any(PacienteRequest.class)))
                .thenThrow(new ConflictException("Ya existe un paciente registrado con el documento 1002003004"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void buscarPorId_debeRetornar200_cuandoElPacienteExiste() throws Exception {
        UUID id = UUID.randomUUID();
        PacienteResponse response = new PacienteResponse(id, "Juan Perez", "1002003004", "3001234567",
                "juan.perez@mail.com", null, Instant.now());
        when(pacienteService.buscarPorId(id)).thenReturn(response);

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_debeRetornar404_cuandoElPacienteNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(pacienteService.buscarPorId(id))
                .thenThrow(new ResourceNotFoundException("No existe un paciente con id " + id));

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorDocumentoIdentidad_debeRetornar200_cuandoElPacienteExiste() throws Exception {
        PacienteResponse response = new PacienteResponse(UUID.randomUUID(), "Juan Perez", "1002003004",
                "3001234567", "juan.perez@mail.com", null, Instant.now());
        when(pacienteService.buscarPorDocumentoIdentidad("1002003004")).thenReturn(response);

        mockMvc.perform(get("/api/pacientes/documento/{documentoIdentidad}", "1002003004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoIdentidad").value("1002003004"));
    }

    @Test
    void buscarPorDocumentoIdentidad_debeRetornar404_cuandoNoExisteElPaciente() throws Exception {
        when(pacienteService.buscarPorDocumentoIdentidad("9999999"))
                .thenThrow(new ResourceNotFoundException("No existe un paciente con documento de identidad 9999999"));

        mockMvc.perform(get("/api/pacientes/documento/{documentoIdentidad}", "9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_debeRetornar200ConLaListaDePacientes() throws Exception {
        PacienteResponse response = new PacienteResponse(UUID.randomUUID(), "Juan Perez", "1002003004",
                "3001234567", "juan.perez@mail.com", null, Instant.now());
        when(pacienteService.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentoIdentidad").value("1002003004"));
    }
}

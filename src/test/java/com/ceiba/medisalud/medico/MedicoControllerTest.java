package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicoController.class)
class MedicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicoService medicoService;

    @Test
    void registrar_debeRetornar201_cuandoElRequestEsValido() throws Exception {
        MedicoRequest request = new MedicoRequest("Dra. Maria Gonzalez", "Cardiologia", "555-1001",
                "maria.gonzalez@medisalud.com");
        MedicoResponse response = new MedicoResponse(UUID.randomUUID(), "Dra. Maria Gonzalez", "Cardiologia",
                "555-1001", "maria.gonzalez@medisalud.com", Instant.now());
        when(medicoService.registrar(any(MedicoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/medicos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCompleto").value("Dra. Maria Gonzalez"))
                .andExpect(jsonPath("$.especialidad").value("Cardiologia"));
    }

    @Test
    void registrar_debeRetornar400_cuandoElNombreEsMuyCorto() throws Exception {
        MedicoRequest request = new MedicoRequest("Al", "Cardiologia", null, null);

        mockMvc.perform(post("/api/medicos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("nombreCompleto"));
    }

    @Test
    void registrar_debeRetornar400_cuandoElTelefonoTieneFormatoInvalido() throws Exception {
        MedicoRequest request = new MedicoRequest("Dra. Maria Gonzalez", "Cardiologia", "abc", null);

        mockMvc.perform(post("/api/medicos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarPorId_debeRetornar200_cuandoElMedicoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        MedicoResponse response = new MedicoResponse(id, "Dr. Carlos Ruiz", "Pediatria", "555-1002",
                "carlos.ruiz@medisalud.com", Instant.now());
        when(medicoService.buscarPorId(id)).thenReturn(response);

        mockMvc.perform(get("/api/medicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_debeRetornar404_cuandoElMedicoNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicoService.buscarPorId(id)).thenThrow(new ResourceNotFoundException("No existe un medico con id " + id));

        mockMvc.perform(get("/api/medicos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listar_debeRetornar200ConLaListaDeMedicos() throws Exception {
        MedicoResponse response = new MedicoResponse(UUID.randomUUID(), "Dra. Ana Lopez", "Dermatologia",
                null, null, Instant.now());
        when(medicoService.listar(eq("Dermatologia"))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/medicos").param("especialidad", "Dermatologia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].especialidad").value("Dermatologia"));
    }
}

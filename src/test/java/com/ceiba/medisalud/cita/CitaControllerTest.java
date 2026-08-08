package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CancelacionResponse;
import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.cita.dto.DisponibilidadResponse;
import com.ceiba.medisalud.cita.dto.FranjaDisponibleResponse;
import com.ceiba.medisalud.shared.exception.BusinessRuleException;
import com.ceiba.medisalud.shared.exception.ConflictException;
import com.ceiba.medisalud.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CitaService citaService;

    @Test
    void reservar_debeRetornar201_cuandoElRequestEsValido() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        LocalDateTime fechaHora = LocalDateTime.of(2026, 8, 10, 9, 0);
        CitaRequest request = new CitaRequest(pacienteId, medicoId, fechaHora);
        CitaResponse response = new CitaResponse(UUID.randomUUID(), pacienteId, "Juan Perez", medicoId,
                "Dra. Maria Gonzalez", "Cardiologia", fechaHora, EstadoCita.PROGRAMADA, null, Instant.now());
        when(citaService.reservar(any(CitaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/citas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    void reservar_debeRetornar400_cuandoFaltaElPaciente() throws Exception {
        String body = """
                {"medicoId": "%s", "fechaHora": "2026-08-10T09:00:00"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/citas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("pacienteId"));
    }

    @Test
    void reservar_debeRetornar404_cuandoElPacienteNoExiste() throws Exception {
        CitaRequest request = new CitaRequest(UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 10, 9, 0));
        when(citaService.reservar(any(CitaRequest.class)))
                .thenThrow(new ResourceNotFoundException("No existe un paciente con id " + request.pacienteId()));

        mockMvc.perform(post("/api/citas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservar_debeRetornar409_cuandoLaFranjaYaEstaOcupada() throws Exception {
        CitaRequest request = new CitaRequest(UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 10, 9, 0));
        when(citaService.reservar(any(CitaRequest.class)))
                .thenThrow(new ConflictException("El medico ya tiene una cita programada en esa franja horaria"));

        mockMvc.perform(post("/api/citas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void reservar_debeRetornar400_cuandoLaFranjaNoEsValida() throws Exception {
        CitaRequest request = new CitaRequest(UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 9, 9, 0));
        when(citaService.reservar(any(CitaRequest.class)))
                .thenThrow(new BusinessRuleException("La fecha y hora debe corresponder a una franja valida"));

        mockMvc.perform(post("/api/citas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarPorId_debeRetornar200_cuandoLaCitaExiste() throws Exception {
        UUID id = UUID.randomUUID();
        CitaResponse response = new CitaResponse(id, UUID.randomUUID(), "Juan Perez", UUID.randomUUID(),
                "Dra. Maria Gonzalez", "Cardiologia", LocalDateTime.of(2026, 8, 10, 9, 0),
                EstadoCita.PROGRAMADA, null, Instant.now());
        when(citaService.buscarPorId(id)).thenReturn(response);

        mockMvc.perform(get("/api/citas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_debeRetornar404_cuandoLaCitaNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(citaService.buscarPorId(id)).thenThrow(new ResourceNotFoundException("No existe una cita con id " + id));

        mockMvc.perform(get("/api/citas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelar_debeRetornar200_conElEstadoCancelada() throws Exception {
        UUID id = UUID.randomUUID();
        CancelacionResponse response = new CancelacionResponse(id, EstadoCita.CANCELADA, Instant.now(), false);
        when(citaService.cancelar(id)).thenReturn(response);

        mockMvc.perform(put("/api/citas/{id}/cancelar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.penalizacionRegistrada").value(false));
    }

    @Test
    void cancelar_debeRetornar404_cuandoLaCitaNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(citaService.cancelar(id)).thenThrow(new ResourceNotFoundException("No existe una cita con id " + id));

        mockMvc.perform(put("/api/citas/{id}/cancelar", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelar_debeRetornar409_cuandoLaCitaYaEstaCancelada() throws Exception {
        UUID id = UUID.randomUUID();
        when(citaService.cancelar(id))
                .thenThrow(new ConflictException("Solo se pueden cancelar citas en estado PROGRAMADA"));

        mockMvc.perform(put("/api/citas/{id}/cancelar", id))
                .andExpect(status().isConflict());
    }

    @Test
    void atender_debeRetornar200_conElEstadoAtendida() throws Exception {
        UUID id = UUID.randomUUID();
        CitaResponse response = new CitaResponse(id, UUID.randomUUID(), "Juan Perez", UUID.randomUUID(),
                "Dra. Maria Gonzalez", "Cardiologia", LocalDateTime.of(2026, 8, 10, 9, 0),
                EstadoCita.ATENDIDA, null, Instant.now());
        when(citaService.atender(id)).thenReturn(response);

        mockMvc.perform(put("/api/citas/{id}/atender", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
    }

    @Test
    void atender_debeRetornar404_cuandoLaCitaNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(citaService.atender(id)).thenThrow(new ResourceNotFoundException("No existe una cita con id " + id));

        mockMvc.perform(put("/api/citas/{id}/atender", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void atender_debeRetornar409_cuandoLaCitaNoEstaProgramada() throws Exception {
        UUID id = UUID.randomUUID();
        when(citaService.atender(id))
                .thenThrow(new ConflictException("Solo se pueden marcar como atendidas citas en estado PROGRAMADA"));

        mockMvc.perform(put("/api/citas/{id}/atender", id))
                .andExpect(status().isConflict());
    }

    @Test
    void listar_debeRetornar200_conLaListaDeCitasFiltradas() throws Exception {
        UUID medicoId = UUID.randomUUID();
        CitaResponse response = new CitaResponse(UUID.randomUUID(), UUID.randomUUID(), "Juan Perez", medicoId,
                "Dra. Maria Gonzalez", "Cardiologia", LocalDateTime.of(2026, 8, 10, 9, 0),
                EstadoCita.PROGRAMADA, null, Instant.now());
        when(citaService.listar(medicoId, null, EstadoCita.PROGRAMADA, null, null))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/citas")
                        .param("medicoId", medicoId.toString())
                        .param("estado", "PROGRAMADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicoId").value(medicoId.toString()));
    }

    @Test
    void listar_debeRetornar200_conListaVacia_cuandoNoHayFiltros() throws Exception {
        when(citaService.listar(null, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listar_debeRetornar400_cuandoFechaFinEsAnteriorAFechaInicio() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 8, 10);
        LocalDate fin = LocalDate.of(2026, 8, 5);
        when(citaService.listar(null, null, null, inicio, fin))
                .thenThrow(new BusinessRuleException("La fecha fin no puede ser anterior a la fecha inicio"));

        mockMvc.perform(get("/api/citas")
                        .param("fechaInicio", inicio.toString())
                        .param("fechaFin", fin.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarDisponibilidad_debeRetornar200_conLasFranjasDisponibles() throws Exception {
        UUID medicoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 8, 10);
        DisponibilidadResponse response = new DisponibilidadResponse(medicoId, "Dra. Maria Gonzalez", "Cardiologia",
                fecha, fecha, List.of(new FranjaDisponibleResponse(
                        LocalDateTime.of(fecha, java.time.LocalTime.of(9, 0)),
                        LocalDateTime.of(fecha, java.time.LocalTime.of(9, 30)))));
        when(citaService.consultarDisponibilidad(medicoId, fecha, fecha)).thenReturn(response);

        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("medicoId", medicoId.toString())
                        .param("fechaInicio", fecha.toString())
                        .param("fechaFin", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjasDisponibles[0].horaInicio").value("2026-08-10T09:00:00"));
    }

    @Test
    void consultarDisponibilidad_debeRetornar404_cuandoElMedicoNoExiste() throws Exception {
        UUID medicoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 8, 10);
        when(citaService.consultarDisponibilidad(medicoId, fecha, fecha))
                .thenThrow(new ResourceNotFoundException("No existe un medico con id " + medicoId));

        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("medicoId", medicoId.toString())
                        .param("fechaInicio", fecha.toString())
                        .param("fechaFin", fecha.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarDisponibilidad_debeRetornar400_cuandoFaltaElParametroMedicoId() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 8, 10);

        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("fechaInicio", fecha.toString())
                        .param("fechaFin", fecha.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parametro obligatorio 'medicoId'"));
    }
}

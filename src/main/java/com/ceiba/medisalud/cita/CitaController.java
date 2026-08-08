package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CancelacionResponse;
import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import com.ceiba.medisalud.cita.dto.DisponibilidadResponse;
import com.ceiba.medisalud.cita.dto.ReprogramacionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/citas")
@Tag(name = "Citas", description = "Reserva y consulta de citas medicas")
class CitaController {

    private final CitaService citaService;

    CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    @Operation(summary = "Reservar una cita")
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody CitaRequest request) {
        CitaResponse creada = citaService.reservar(request);
        return ResponseEntity.created(URI.create("/api/citas/" + creada.id())).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cita por id")
    public ResponseEntity<CitaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar citas con filtros opcionales (medico, paciente, estado, rango de fechas)")
    public ResponseEntity<List<CitaResponse>> listar(
            @RequestParam(required = false) UUID medicoId,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(citaService.listar(medicoId, pacienteId, estado, fechaInicio, fechaFin));
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una cita programada")
    public ResponseEntity<CancelacionResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.cancelar(id));
    }

    @PutMapping("/{id}/reprogramar")
    @Operation(summary = "Reprogramar una cita: cancela la actual (aplicando RN-05 si corresponde) y crea "
            + "una nueva con el horario indicado, validando disponibilidad (RN-02 y RN-04)")
    public ResponseEntity<CitaResponse> reprogramar(@PathVariable UUID id,
                                                      @Valid @RequestBody ReprogramacionRequest request) {
        CitaResponse nuevaCita = citaService.reprogramar(id, request.nuevaFechaHora());
        return ResponseEntity.created(URI.create("/api/citas/" + nuevaCita.id())).body(nuevaCita);
    }

    @PutMapping("/{id}/atender")
    @Operation(summary = "Marcar una cita programada como atendida (no forma parte del enunciado original; "
            + "se agrego para que el estado ATENDIDA, ya usado como filtro en RF-06, sea alcanzable)")
    public ResponseEntity<CitaResponse> atender(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.atender(id));
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Consultar las franjas horarias disponibles de un medico en un rango de fechas")
    public ResponseEntity<DisponibilidadResponse> consultarDisponibilidad(
            @RequestParam UUID medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(citaService.consultarDisponibilidad(medicoId, fechaInicio, fechaFin));
    }
}

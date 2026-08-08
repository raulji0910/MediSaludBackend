package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.cita.dto.CitaRequest;
import com.ceiba.medisalud.cita.dto.CitaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
}

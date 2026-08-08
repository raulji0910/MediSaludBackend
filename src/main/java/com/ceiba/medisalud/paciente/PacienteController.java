package com.ceiba.medisalud.paciente;

import com.ceiba.medisalud.paciente.dto.PacienteRequest;
import com.ceiba.medisalud.paciente.dto.PacienteResponse;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes", description = "Registro y consulta de pacientes")
class PacienteController {

    private final PacienteService pacienteService;

    PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    @Operation(summary = "Registrar un paciente")
    public ResponseEntity<PacienteResponse> registrar(@Valid @RequestBody PacienteRequest request) {
        PacienteResponse creado = pacienteService.registrar(request);
        return ResponseEntity.created(URI.create("/api/pacientes/" + creado.id())).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un paciente por id")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @GetMapping("/documento/{documentoIdentidad}")
    @Operation(summary = "Consultar un paciente por su documento de identidad")
    public ResponseEntity<PacienteResponse> buscarPorDocumentoIdentidad(@PathVariable String documentoIdentidad) {
        return ResponseEntity.ok(pacienteService.buscarPorDocumentoIdentidad(documentoIdentidad));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pacientes")
    public ResponseEntity<List<PacienteResponse>> listar() {
        return ResponseEntity.ok(pacienteService.listar());
    }
}

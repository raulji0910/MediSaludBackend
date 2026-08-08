package com.ceiba.medisalud.medico;

import com.ceiba.medisalud.medico.dto.MedicoRequest;
import com.ceiba.medisalud.medico.dto.MedicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicos")
@Tag(name = "Medicos", description = "Registro y consulta de medicos")
class MedicoController {

    private final MedicoService medicoService;

    MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @PostMapping
    @Operation(summary = "Registrar un medico")
    public ResponseEntity<MedicoResponse> registrar(@Valid @RequestBody MedicoRequest request) {
        MedicoResponse creado = medicoService.registrar(request);
        return ResponseEntity.created(URI.create("/api/medicos/" + creado.id())).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un medico por id")
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(medicoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar medicos, opcionalmente filtrados por especialidad")
    public ResponseEntity<List<MedicoResponse>> listar(
            @Parameter(description = "Filtro opcional por especialidad")
            @RequestParam(required = false) String especialidad) {
        return ResponseEntity.ok(medicoService.listar(especialidad));
    }
}

package com.ceiba.medisalud.cita;

import com.ceiba.medisalud.paciente.Paciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "penalizaciones")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Penalizacion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "cita_id")
    private UUID citaId;

    @Column(name = "fecha_penalizacion", nullable = false)
    private Instant fechaPenalizacion;

    @Column(name = "motivo", length = 255)
    private String motivo;

    public Penalizacion(Paciente paciente, UUID citaId, String motivo) {
        this.id = UUID.randomUUID();
        this.paciente = paciente;
        this.citaId = citaId;
        this.fechaPenalizacion = Instant.now();
        this.motivo = motivo;
    }
}

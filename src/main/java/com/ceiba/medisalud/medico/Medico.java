package com.ceiba.medisalud.medico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medico {

    @Id
    private UUID id;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "especialidad", nullable = false, length = 100)
    private String especialidad;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Medico(String nombreCompleto, String especialidad, String telefono, String email) {
        this.id = UUID.randomUUID();
        this.nombreCompleto = nombreCompleto;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.email = email;
        this.creadoEn = Instant.now();
    }
}

package com.ceiba.medisalud.paciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paciente {

    @Id
    private UUID id;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "documento_identidad", nullable = false, length = 30, unique = true)
    private String documentoIdentidad;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Paciente(String nombreCompleto, String documentoIdentidad, String telefono, String email,
                     LocalDate fechaNacimiento) {
        this.id = UUID.randomUUID();
        this.nombreCompleto = nombreCompleto;
        this.documentoIdentidad = documentoIdentidad;
        this.telefono = telefono;
        this.email = email;
        this.fechaNacimiento = fechaNacimiento;
        this.creadoEn = Instant.now();
    }
}

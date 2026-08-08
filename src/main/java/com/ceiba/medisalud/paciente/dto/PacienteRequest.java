package com.ceiba.medisalud.paciente.dto;

import com.ceiba.medisalud.shared.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(

        @NotBlank(message = "el nombre completo es obligatorio")
        @Size(min = 3, max = 100, message = "el nombre completo debe tener entre 3 y 100 caracteres")
        String nombreCompleto,

        @NotBlank(message = "el documento de identidad es obligatorio")
        @Size(min = 7, max = 30, message = "el documento de identidad debe tener al menos 7 caracteres")
        String documentoIdentidad,

        @NotBlank(message = "el telefono es obligatorio")
        @PhoneNumber
        String telefono,

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato valido")
        String email,

        @PastOrPresent(message = "la fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento
) {
}

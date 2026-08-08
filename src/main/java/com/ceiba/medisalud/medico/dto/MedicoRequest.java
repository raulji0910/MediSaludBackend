package com.ceiba.medisalud.medico.dto;

import com.ceiba.medisalud.shared.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicoRequest(

        @NotBlank(message = "el nombre completo es obligatorio")
        @Size(min = 3, max = 100, message = "el nombre completo debe tener entre 3 y 100 caracteres")
        String nombreCompleto,

        @NotBlank(message = "la especialidad es obligatoria")
        @Size(max = 100, message = "la especialidad no puede superar los 100 caracteres")
        String especialidad,

        @PhoneNumber
        String telefono,

        @Email(message = "el email no tiene un formato valido")
        String email
) {
}

package com.ceiba.medisalud.cita;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "medisalud.citas")
public record CitaProperties(
        int franjaMinutos,
        Horario horario,
        Penalizacion penalizacion
) {

    public record Horario(
            LocalTime apertura,
            LocalTime cierreEntreSemana,
            LocalTime cierreSabado
    ) {
    }

    public record Penalizacion(
            int horasMinimasCancelacion,
            int diasVentana,
            int maxPermitidas
    ) {
    }
}

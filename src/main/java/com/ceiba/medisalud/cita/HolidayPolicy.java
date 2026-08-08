package com.ceiba.medisalud.cita;

import java.time.LocalDate;

/**
 * Punto de extension para el calendario de festivos (RN-01). El enunciado no provee
 * un listado de festivos, por lo que la implementacion por defecto no marca ningun dia
 * como festivo; conectar un calendario real solo requiere otra implementacion de esta
 * interfaz, sin tocar {@link HorarioAtencionPolicy}.
 */
public interface HolidayPolicy {

    boolean esFestivo(LocalDate fecha);
}

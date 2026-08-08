package com.ceiba.medisalud.cita;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class HorarioAtencionPolicy {

    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE_ENTRE_SEMANA = LocalTime.of(18, 0);
    private static final LocalTime CIERRE_SABADO = LocalTime.of(13, 0);
    private static final int DURACION_FRANJA_MINUTOS = 30;

    private final HolidayPolicy holidayPolicy;

    public HorarioAtencionPolicy(HolidayPolicy holidayPolicy) {
        this.holidayPolicy = holidayPolicy;
    }

    public boolean esFranjaValida(LocalDateTime fechaHora) {
        if (fechaHora.getSecond() != 0 || fechaHora.getNano() != 0
                || fechaHora.getMinute() % DURACION_FRANJA_MINUTOS != 0) {
            return false;
        }

        LocalDate fecha = fechaHora.toLocalDate();
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY || holidayPolicy.esFestivo(fecha)) {
            return false;
        }

        LocalTime cierre = dia == DayOfWeek.SATURDAY ? CIERRE_SABADO : CIERRE_ENTRE_SEMANA;
        LocalTime hora = fechaHora.toLocalTime();
        return !hora.isBefore(APERTURA) && !hora.plusMinutes(DURACION_FRANJA_MINUTOS).isAfter(cierre);
    }
}

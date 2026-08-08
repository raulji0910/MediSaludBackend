package com.ceiba.medisalud.cita;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class HorarioAtencionPolicy {

    public static final int DURACION_FRANJA_MINUTOS = 30;

    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE_ENTRE_SEMANA = LocalTime.of(18, 0);
    private static final LocalTime CIERRE_SABADO = LocalTime.of(13, 0);

    private final HolidayPolicy holidayPolicy;

    public HorarioAtencionPolicy(HolidayPolicy holidayPolicy) {
        this.holidayPolicy = holidayPolicy;
    }

    public boolean esFranjaValida(LocalDateTime fechaHora) {
        if (fechaHora.getSecond() != 0 || fechaHora.getNano() != 0) {
            return false;
        }
        return franjasDelDia(fechaHora.toLocalDate()).contains(fechaHora.toLocalTime());
    }

    public List<LocalTime> franjasDelDia(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY || holidayPolicy.esFestivo(fecha)) {
            return List.of();
        }

        LocalTime cierre = dia == DayOfWeek.SATURDAY ? CIERRE_SABADO : CIERRE_ENTRE_SEMANA;
        List<LocalTime> franjas = new ArrayList<>();
        for (LocalTime hora = APERTURA; !hora.plusMinutes(DURACION_FRANJA_MINUTOS).isAfter(cierre);
                hora = hora.plusMinutes(DURACION_FRANJA_MINUTOS)) {
            franjas.add(hora);
        }
        return franjas;
    }
}

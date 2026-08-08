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

    private final HolidayPolicy holidayPolicy;
    private final CitaProperties citaProperties;

    public HorarioAtencionPolicy(HolidayPolicy holidayPolicy, CitaProperties citaProperties) {
        this.holidayPolicy = holidayPolicy;
        this.citaProperties = citaProperties;
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

        CitaProperties.Horario horario = citaProperties.horario();
        LocalTime cierre = dia == DayOfWeek.SATURDAY ? horario.cierreSabado() : horario.cierreEntreSemana();
        int duracionFranjaMinutos = citaProperties.franjaMinutos();

        List<LocalTime> franjas = new ArrayList<>();
        for (LocalTime hora = horario.apertura(); !hora.plusMinutes(duracionFranjaMinutos).isAfter(cierre);
                hora = hora.plusMinutes(duracionFranjaMinutos)) {
            franjas.add(hora);
        }
        return franjas;
    }
}

package com.ceiba.medisalud.cita;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
class SinFestivosConfiguradosPolicy implements HolidayPolicy {

    @Override
    public boolean esFestivo(LocalDate fecha) {
        return false;
    }
}

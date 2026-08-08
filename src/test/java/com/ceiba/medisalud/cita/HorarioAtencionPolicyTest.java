package com.ceiba.medisalud.cita;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HorarioAtencionPolicyTest {

    private static final LocalDate LUNES = LocalDate.of(2024, 1, 1);
    private static final LocalDate SABADO = LocalDate.of(2024, 1, 6);
    private static final LocalDate DOMINGO = LocalDate.of(2024, 1, 7);

    static CitaProperties propiedadesPorDefecto() {
        return new CitaProperties(30,
                new CitaProperties.Horario(LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(13, 0)),
                new CitaProperties.Penalizacion(2, 30, 3));
    }

    private final HorarioAtencionPolicy policy =
            new HorarioAtencionPolicy(new SinFestivosConfiguradosPolicy(), propiedadesPorDefecto());

    @Test
    void esFranjaValida_debeSerValida_alInicioDeLaJornadaEntreSemana() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(8, 0)))).isTrue();
    }

    @Test
    void esFranjaValida_debeSerValida_enLaUltimaFranjaEntreSemana() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(17, 30)))).isTrue();
    }

    @Test
    void esFranjaValida_debeSerInvalida_alCierreExactoEntreSemana() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(18, 0)))).isFalse();
    }

    @Test
    void esFranjaValida_debeSerInvalida_antesDeLaApertura() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(7, 30)))).isFalse();
    }

    @Test
    void esFranjaValida_debeSerValida_alInicioDeLaJornadaSabado() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(SABADO, LocalTime.of(8, 0)))).isTrue();
    }

    @Test
    void esFranjaValida_debeSerValida_enLaUltimaFranjaSabado() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(SABADO, LocalTime.of(12, 30)))).isTrue();
    }

    @Test
    void esFranjaValida_debeSerInvalida_despuesDelCierreSabado() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(SABADO, LocalTime.of(13, 0)))).isFalse();
    }

    @Test
    void esFranjaValida_debeSerInvalida_enDomingo() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(DOMINGO, LocalTime.of(10, 0)))).isFalse();
    }

    @Test
    void esFranjaValida_debeSerInvalida_conMinutosNoAlineadosA30() {
        assertThat(policy.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(9, 15)))).isFalse();
    }

    @Test
    void esFranjaValida_debeSerInvalida_cuandoElDiaEsFestivo() {
        HolidayPolicy festivos = mock(HolidayPolicy.class);
        when(festivos.esFestivo(any())).thenReturn(true);
        HorarioAtencionPolicy policyConFestivos = new HorarioAtencionPolicy(festivos, propiedadesPorDefecto());

        assertThat(policyConFestivos.esFranjaValida(LocalDateTime.of(LUNES, LocalTime.of(9, 0)))).isFalse();
    }

    @Test
    void franjasDelDia_debeRetornar20Franjas_enUnDiaEntreSemana() {
        assertThat(policy.franjasDelDia(LUNES))
                .hasSize(20)
                .startsWith(LocalTime.of(8, 0))
                .endsWith(LocalTime.of(17, 30));
    }

    @Test
    void franjasDelDia_debeRetornar10Franjas_enSabado() {
        assertThat(policy.franjasDelDia(SABADO))
                .hasSize(10)
                .startsWith(LocalTime.of(8, 0))
                .endsWith(LocalTime.of(12, 30));
    }

    @Test
    void franjasDelDia_debeRetornarVacio_enDomingo() {
        assertThat(policy.franjasDelDia(DOMINGO)).isEmpty();
    }

    @Test
    void franjasDelDia_debeRetornarVacio_cuandoElDiaEsFestivo() {
        HolidayPolicy festivos = mock(HolidayPolicy.class);
        when(festivos.esFestivo(any())).thenReturn(true);
        HorarioAtencionPolicy policyConFestivos = new HorarioAtencionPolicy(festivos, propiedadesPorDefecto());

        assertThat(policyConFestivos.franjasDelDia(LUNES)).isEmpty();
    }
}

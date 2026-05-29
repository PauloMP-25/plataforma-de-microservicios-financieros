package com.libreria.comun.utilidades;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Estrategia de cálculo que suma un mes a la fecha de referencia y,
 * en caso de caer fin de semana (sábado o domingo), mueve la fecha al lunes siguiente.
 */
public class CalculadorFechasDiasHabiles implements CalculadorFechasStrategy {

    @Override
    public LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia) {
        LocalDate base = fechaReferencia != null ? fechaReferencia.plusMonths(1) : LocalDate.now().plusMonths(1);
        
        if (base.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return base.plusDays(2);
        } else if (base.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return base.plusDays(1);
        }
        return base;
    }

    @Override
    public boolean soporta(String tipoEstrategia) {
        return "DIAS_HABILES".equalsIgnoreCase(tipoEstrategia);
    }
}

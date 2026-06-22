package com.libreria.comun.utilidades;

import java.time.LocalDate;

/**
 * Estrategia de cálculo que suma exactamente un mes a la fecha de referencia,
 * independientemente del día de la semana.
 */
public class CalculadorFechasCalendario implements CalculadorFechasStrategy {

    @Override
    public LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            return LocalDate.now().plusMonths(1);
        }
        return fechaReferencia.plusMonths(1);
    }

    @Override
    public boolean soporta(String tipoEstrategia) {
        return "CALENDARIO".equalsIgnoreCase(tipoEstrategia) || "MENSUAL".equalsIgnoreCase(tipoEstrategia);
    }
}

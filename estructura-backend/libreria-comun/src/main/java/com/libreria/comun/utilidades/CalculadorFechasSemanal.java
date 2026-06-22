package com.libreria.comun.utilidades;

import java.time.LocalDate;

/**
 * Estrategia de cálculo que suma exactamente una semana a la fecha de referencia.
 */
public class CalculadorFechasSemanal implements CalculadorFechasStrategy {

    @Override
    public LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            return LocalDate.now().plusWeeks(1);
        }
        return fechaReferencia.plusWeeks(1);
    }

    @Override
    public boolean soporta(String tipoEstrategia) {
        return "SEMANAL".equalsIgnoreCase(tipoEstrategia);
    }
}

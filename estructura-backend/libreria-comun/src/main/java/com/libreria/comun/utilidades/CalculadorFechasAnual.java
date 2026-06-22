package com.libreria.comun.utilidades;

import java.time.LocalDate;

/**
 * Estrategia de cálculo que suma exactamente un año a la fecha de referencia.
 */
public class CalculadorFechasAnual implements CalculadorFechasStrategy {

    @Override
    public LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            return LocalDate.now().plusYears(1);
        }
        return fechaReferencia.plusYears(1);
    }

    @Override
    public boolean soporta(String tipoEstrategia) {
        return "ANUAL".equalsIgnoreCase(tipoEstrategia);
    }
}

package com.libreria.comun.utilidades;

import java.time.LocalDate;

/**
 * Estrategia de cálculo que suma exactamente dos semanas (quince días aproximados) a la fecha de referencia.
 */
public class CalculadorFechasQuincenal implements CalculadorFechasStrategy {

    @Override
    public LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            return LocalDate.now().plusWeeks(2);
        }
        return fechaReferencia.plusWeeks(2);
    }

    @Override
    public boolean soporta(String tipoEstrategia) {
        return "QUINCENAL".equalsIgnoreCase(tipoEstrategia);
    }
}

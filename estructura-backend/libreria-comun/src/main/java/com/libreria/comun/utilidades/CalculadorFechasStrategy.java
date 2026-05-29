package com.libreria.comun.utilidades;

import java.time.LocalDate;

/**
 * Estrategia para calcular la fecha del siguiente cobro de una suscripción.
 */
public interface CalculadorFechasStrategy {

    /**
     * Calcula la siguiente fecha de pago a partir de una fecha de referencia.
     *
     * @param fechaReferencia Fecha de referencia (normalmente la fecha de hoy o del último pago).
     * @return La siguiente fecha de vencimiento/cobro.
     */
    LocalDate calcularSiguienteFechaPago(LocalDate fechaReferencia);

    /**
     * Determina si esta estrategia soporta el tipo especificado.
     *
     * @param tipoEstrategia Nombre identificador de la estrategia.
     * @return true si soporta el tipo, false de lo contrario.
     */
    boolean soporta(String tipoEstrategia);
}

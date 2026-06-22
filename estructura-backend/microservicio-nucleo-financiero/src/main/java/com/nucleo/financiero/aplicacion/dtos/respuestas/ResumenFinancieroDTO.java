package com.nucleo.financiero.aplicacion.dtos.respuestas;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;

/**
 * DTO que consolida la información financiera de un periodo específico.
 *
 * @param desde            Fecha inicial del periodo.
 * @param hasta            Fecha final del periodo.
 * @param totalIngresos    Suma de todos los ingresos.
 * @param totalGastos      Suma de todos los gastos.
 * @param balance          Diferencia neta (Ingresos - Gastos).
 * @param cantidadIngresos Número de operaciones de ingreso.
 * @param cantidadGastos   Número de operaciones de gasto.
 */
@Builder
public record ResumenFinancieroDTO(
        LocalDateTime desde,
        LocalDateTime hasta,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal balance,
        long cantidadIngresos,
        long cantidadGastos
) {
    /**
     * Calcula un resumen financiero a partir de valores base.
     * 
     * @param desde Fecha inicio.
     * @param hasta Fecha fin.
     * @param totalIngresos Suma de ingresos.
     * @param totalGastos Suma de gastos.
     * @param cantidadIngresos Conteo de ingresos.
     * @param cantidadGastos Conteo de gastos.
     * @return DTO consolidado con balance calculado.
     */
    public static ResumenFinancieroDTO calcular(
            LocalDateTime desde, LocalDateTime hasta,
            BigDecimal totalIngresos, BigDecimal totalGastos,
            long cantidadIngresos, long cantidadGastos) {
        
        BigDecimal ingresos = totalIngresos != null ? totalIngresos.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal gastos = totalGastos != null ? totalGastos.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return ResumenFinancieroDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .totalIngresos(ingresos)
                .totalGastos(gastos)
                .balance(ingresos.subtract(gastos).setScale(2, RoundingMode.HALF_UP))
                .cantidadIngresos(cantidadIngresos)
                .cantidadGastos(cantidadGastos)
                .build();
    }
}

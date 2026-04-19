package com.nucleo.financiero.aplicacion.dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ResumenFinancieroDTO(
    LocalDateTime desde,
    LocalDateTime hasta,
    BigDecimal totalIngresos,
    BigDecimal totalGastos,
    BigDecimal balance,
    long cantidadIngresos,
    long cantidadGastos,
    long totalTransacciones,
    BigDecimal promedioIngreso,
    BigDecimal promedioGasto
) {
    public static ResumenFinancieroDTO calcular(
            LocalDateTime desde,
            LocalDateTime hasta,
            BigDecimal totalIngresos,
            BigDecimal totalGastos,
            long cantidadIngresos,
            long cantidadGastos) {

        BigDecimal balance = totalIngresos.subtract(totalGastos);
        long totalTransacciones = cantidadIngresos + cantidadGastos;

        BigDecimal promedioIngreso = cantidadIngresos > 0
                ? totalIngresos.divide(BigDecimal.valueOf(cantidadIngresos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal promedioGasto = cantidadGastos > 0
                ? totalGastos.divide(BigDecimal.valueOf(cantidadGastos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ResumenFinancieroDTO(
                desde, hasta,
                totalIngresos, totalGastos, balance,
                cantidadIngresos, cantidadGastos, totalTransacciones,
                promedioIngreso, promedioGasto
        );
    }
}

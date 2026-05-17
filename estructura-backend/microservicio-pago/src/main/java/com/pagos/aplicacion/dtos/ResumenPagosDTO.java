package com.pagos.aplicacion.dtos;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resumen financiero para el panel de administración.
 */
public record ResumenPagosDTO(
    long totalTransacciones,
    BigDecimal ingresosTotales,
    Map<String, Long> transaccionesPorEstado,
    Map<String, Long> suscripcionesPorPlan
) {}

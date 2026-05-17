package com.pagos.aplicacion.enums;

import lombok.Getter;
import java.math.BigDecimal;

/**
 * Define los planes de suscripción disponibles en LUKA APP.
 */
@Getter
public enum PlanSuscripcion {
    FREE(BigDecimal.ZERO, "Plan gratuito con funcionalidades básicas"),
    PREMIUM(new BigDecimal("19.90"), "Plan premium con análisis avanzado"),
    PRO(new BigDecimal("39.90"), "Plan profesional con todas las herramientas de coaching");

    private final BigDecimal precio;
    private final String descripcion;

    PlanSuscripcion(BigDecimal precio, String descripcion) {
        this.precio = precio;
        this.descripcion = descripcion;
    }
}

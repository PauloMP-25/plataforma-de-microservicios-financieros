package com.nucleo.financiero.aplicacion.dtos.ia;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum que representa los módulos de análisis disponibles en el
 * microservicio-ia (Python/FastAPI).
 *
 * CRÍTICO: Los valores de este enum DEBEN coincidir exactamente con los
 * valores del TipoModulo en Python (evento_analisis.py).
 * Python usa SNAKE_CASE con mayúsculas: GASTO_HORMIGA, PREDICCION_GASTOS, etc.
 *
 * @JsonValue garantiza que Jackson serialice el .name() exacto al JSON,
 * sin importar el nombre del campo Java.
 */
public enum ModuloIa {

    TRANSACCION_AUTOMATICA,
    PREDICCION_GASTOS,
    GASTO_HORMIGA,
    AUTOCLASIFICACION,
    COMPARACION_MENSUAL,
    CAPACIDAD_AHORRO,
    METAS_FINANCIERAS,
    ANOMALIAS,
    ESTACIONALIDAD,
    PRESUPUESTO_DINAMICO,
    REPORTE_COMPLETO;

    /**
     * Serializa el enum como su nombre exacto en SNAKE_CASE.
     * Resultado JSON: "GASTO_HORMIGA", "PREDICCION_GASTOS", etc.
     * Esto hace match exacto con Python's TipoModulo.value
     */
    @JsonValue
    public String toJson() {
        return this.name();
    }
}
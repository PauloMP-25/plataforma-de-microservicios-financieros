package com.libreria.comun.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Módulos específicos de análisis disponibles en el motor de IA.
 * <p>
 * Cada módulo corresponde a una capacidad analítica diferente en el microservicio-ia
 * (Python/Gemini). El @JsonValue asegura que se serialice en el formato que Python espera.
 * </p>
 */
public enum ModuloIa {
    GASTO_HORMIGA,
    COMPARACION_MENSUAL,
    PREDICCION_FLUJO,
    SALUD_FINANCIERA;

    @JsonValue
    public String getValue() {
        return name();
    }
}

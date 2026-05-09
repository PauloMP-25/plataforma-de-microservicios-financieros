package com.libreria.comun.dtos;

import java.util.Map;
import java.util.UUID;

/**
 * Contrato de respuesta generado por el microservicio de IA.
 * <p>
 * Contiene el resultado del procesamiento, consejos generados y metadatos 
 * del estado del modelo de lenguaje.
 * </p>
 * 
 * @param usuarioId    Usuario al que pertenece el análisis.
 * @param resultado    Resumen textual del análisis o consejo del Coach.
 * @param predicciones Mapa de valores predichos (ej: {"comida": 450.0, "ocio": 120.0}).
 * @param estadoCoach  Estado del procesamiento (ej: "EXITOSO", "ERROR_GEMINI").
 */
public record RespuestaIaDTO(
    UUID usuarioId,
    String resultado,
    Map<String, Object> predicciones,
    String estadoCoach
) {}

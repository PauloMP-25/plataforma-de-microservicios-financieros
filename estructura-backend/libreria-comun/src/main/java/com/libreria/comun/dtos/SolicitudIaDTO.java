package com.libreria.comun.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato de solicitud para el análisis de Inteligencia Artificial.
 * <p>
 * Define los parámetros necesarios para que el motor de IA (Gemini/Python) 
 * procese la información financiera de un usuario en un rango de tiempo específico.
 * </p>
 * 
 * @param usuarioId      Identificador único del usuario a analizar.
 * @param tipoAnalisis   Categoría del análisis (ej: "PREDICCION", "CATEGORIZACION", "COACH").
 * @param fechaInicio    Fecha desde la cual se tomarán los datos.
 * @param fechaFin       Fecha hasta la cual se tomarán los datos.
 * @param incluirDetalles Indica si se requiere un desglose profundo en la respuesta.
 * 
 * @author Paulo Moron
 */
public record SolicitudIaDTO(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,

    @NotBlank(message = "El tipo de análisis es obligatorio")
    String tipoAnalisis,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate fechaInicio,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDate fechaFin,

    boolean incluirDetalles
) {}

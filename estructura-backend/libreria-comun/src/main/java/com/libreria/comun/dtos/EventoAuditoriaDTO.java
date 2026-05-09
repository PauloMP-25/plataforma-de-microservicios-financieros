package com.libreria.comun.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO para auditoría de eventos generales del sistema.
 * <p>
 * Utilizado para registrar acciones que no modifican estados financieros 
 * pero que son relevantes para el historial operativo (ej: ejecuciones de la IA).
 * </p>
 */
public record EventoAuditoriaDTO(
    LocalDateTime fechaHora,

    @NotBlank(message = "El usuario es obligatorio")
    String nombreUsuario,

    @NotBlank(message = "La acción es obligatoria")
    String accion,

    @NotBlank(message = "El módulo es obligatorio")
    String modulo,

    @Size(max = 45)
    String ipOrigen,

    String detalles
) {}

package com.libreria.comun.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para auditoría de eventos generales del sistema.
 * <p>
 * Utilizado para registrar acciones que no modifican estados financieros
 * pero que son relevantes para el historial operativo (ej: ejecuciones de la
 * IA).
 * </p>
 */

public record EventoAuditoriaDTO(

        UUID usuarioId,

        @NotBlank(message = "La acción es obligatoria") String accion,

        @NotBlank(message = "El módulo es obligatorio") String modulo,

        @Size(max = 45) String ipOrigen,

        String detalles,

        LocalDate fecha) {
    /**
     * Factory method principal. Deja {@code fechaHora} en null para que el
     * ms-auditoria la asigne en {@code @PrePersist}, garantizando consistencia
     * de zona horaria.
     */
    public static EventoAuditoriaDTO crear(
            UUID usuarioId,
            String accion,
            String modulo,
            String ipOrigen,
            String detalles) {
        return new EventoAuditoriaDTO(
                usuarioId,
                accion,
                modulo,
                ipOrigen,
                detalles,
                LocalDate.now());
    };
}
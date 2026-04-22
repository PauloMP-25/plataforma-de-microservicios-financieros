package com.auditoria.aplicacion.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida para consultas de trazabilidad transaccional.
 */
public record AuditoriaTransaccionalDTO(
    UUID          id,
    UUID          usuarioId,
    String        servicioOrigen,
    String        entidadAfectada,
    String        entidadId,
    String        valorAnterior,
    String        valorNuevo,
    LocalDateTime fecha
) {}

package com.auditoria.aplicacion.dtos;

import com.auditoria.dominio.entidades.AuditoriaAcceso.EstadoAcceso;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida para consultas de auditoría de accesos.
 */
public record AuditoriaAccesoDTO(
    UUID          id,
    UUID          usuarioId,
    String        ipOrigen,
    String        navegador,
    EstadoAcceso  estado,
    String        detalleError,
    LocalDateTime fecha
) {}

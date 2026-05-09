package com.libreria.comun.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de cambios en entidades de negocio (Trazabilidad).
 * <p>
 * Este DTO permite capturar el "antes" y "después" de cualquier registro 
 * en la base de datos, facilitando auditorías financieras y recuperación de datos.
 * </p>
 * 
 * @param usuarioId       Usuario que realizó el cambio.
 * @param servicioOrigen  Nombre del microservicio que genera el evento.
 * @param entidadAfectada Nombre de la tabla o entidad (ej: 'cuenta', 'meta').
 * @param entidadId       ID del registro afectado (en String para soportar UUID o Long).
 * @param valorAnterior   Estado del objeto antes del cambio (formato JSON).
 * @param valorNuevo      Estado del objeto después del cambio (formato JSON).
 * @param fecha           Momento del cambio.
 */
public record EventoTransaccionalDTO(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,

    @NotBlank(message = "El servicio de origen es obligatorio")
    String servicioOrigen,

    @NotBlank(message = "La entidad afectada es obligatoria")
    String entidadAfectada,

    @NotBlank(message = "El ID de la entidad es obligatorio")
    String entidadId,

    String valorAnterior,
    String valorNuevo,
    LocalDateTime fecha
) {}

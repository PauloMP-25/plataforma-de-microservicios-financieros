package com.nucleo.financiero.aplicacion.dtos.auditoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

// ──────────────────────────────────────────────────────────────────────────────
// DTO de ENTRADA
// ──────────────────────────────────────────────────────────────────────────────

/**
 * DTO para registrar un cambio en una entidad de negocio.
 * Lo envían los microservicios cuando modifican datos importantes.
 */
public record AuditoriaTransaccionalRequestDTO(

    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,

    @NotBlank(message = "El servicio de origen es obligatorio")
    String servicioOrigen,

    @NotBlank(message = "La entidad afectada es obligatoria")
    String entidadAfectada,

    @NotBlank(message = "El ID de la entidad es obligatorio")
    String entidadId,

    String valorAnterior, // JSON del estado anterior (puede ser null en creaciones)
    String valorNuevo,    // JSON del estado nuevo (puede ser null en eliminaciones)

    LocalDateTime fecha   // Opcional: se asigna en @PrePersist si no viene
) {}

package com.nucleo.financiero.aplicacion.dtos.auditoria;

// ─── Imports ─────────────────────────────────────────────────────────────────
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

// ══════════════════════════════════════════════════════════════════════════════
// DTOs DE ENTRADA (Request)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * DTO de entrada para registrar un evento de acceso (login/logout).
 * Lo envía el microservicio-usuario o el api-gateway.
 */
public record AuditoriaAccesoRequestDTO(

    UUID usuarioId, // Opcional: null si el usuario no existe

    @NotBlank(message = "La IP de origen es obligatoria")
    @Size(max = 45, message = "IP no puede superar 45 caracteres (IPv6)")
    String ipOrigen,

    @Size(max = 500)
    String navegador,

    @NotNull(message = "El estado (EXITO/FALLO) es obligatorio")
    EstadoAcceso estado,

    @Size(max = 500)
    String detalleError,

    LocalDateTime fecha // Opcional: se asigna en @PrePersist si no viene
) {}

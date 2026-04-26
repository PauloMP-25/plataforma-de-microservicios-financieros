package com.comun.aplicacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaTransaccionalRequestDTO(
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

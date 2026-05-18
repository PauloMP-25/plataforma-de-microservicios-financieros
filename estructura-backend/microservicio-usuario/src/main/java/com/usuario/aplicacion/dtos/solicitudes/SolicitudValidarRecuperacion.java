package com.usuario.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudValidarRecuperacion(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,
        @NotBlank(message = "El código es obligatorio")
        String codigo) {
}

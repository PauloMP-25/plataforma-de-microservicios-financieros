package com.mensajeria.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudRecuperacion(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,
        @NotBlank(message = "El código es obligatorio")
        String codigo) {
}

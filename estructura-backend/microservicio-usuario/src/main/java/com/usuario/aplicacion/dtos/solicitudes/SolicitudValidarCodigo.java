package com.usuario.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SolicitudValidarCodigo(
        @NotNull(message = "El ID de usuario es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El código es obligatorio")
        String codigo
) {
}

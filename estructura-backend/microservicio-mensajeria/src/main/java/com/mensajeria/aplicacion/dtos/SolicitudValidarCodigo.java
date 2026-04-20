package com.mensajeria.aplicacion.dtos;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudValidarCodigo(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,
        @NotBlank(message = "El código es obligatorio")
        String codigo) {

}

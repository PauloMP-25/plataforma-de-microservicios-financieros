package com.mensajeria.aplicacion.dtos;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudGenerarCodigo(
        @NotNull(message = "El usuarioId es obligatorio") UUID usuarioId,

                String email,

                String telefono,

                @NotNull(message = "El tipo de canal es obligatorio") com.libreria.comun.enums.TipoVerificacion tipo,

                @NotNull(message = "El propósito del código es obligatorio") com.libreria.comun.enums.PropositoCodigo proposito) {
}

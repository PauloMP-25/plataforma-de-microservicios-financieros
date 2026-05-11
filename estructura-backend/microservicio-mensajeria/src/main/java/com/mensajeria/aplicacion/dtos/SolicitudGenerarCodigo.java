package com.mensajeria.aplicacion.dtos;

import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudGenerarCodigo(
        @NotNull(message = "El usuarioId es obligatorio") UUID usuarioId,

        @NotBlank(message = "El email es obligatorio") @Email(message = "Formato de email inválido") String email,

        @NotBlank(message = "El telefono es obligatorio") @Size(max = 9, message = "El teléfono no puede tener más de 9 caracteres") String telefono,

        @NotNull(message = "El tipo de canal es obligatorio") TipoVerificacion tipo,

        @NotNull(message = "El propósito del código es obligatorio") PropositoCodigo proposito) {
}

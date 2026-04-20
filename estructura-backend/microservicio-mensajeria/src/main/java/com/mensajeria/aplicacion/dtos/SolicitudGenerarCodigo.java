package com.mensajeria.aplicacion.dtos;

import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudGenerarCodigo(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    String email,

    String telefono,

    @NotNull(message = "El tipo de canal es obligatorio")
    TipoVerificacion tipo,

    @NotNull(message = "El propósito del código es obligatorio")
    PropositoCodigo proposito // ACTIVACION_CUENTA o RESTABLECER_PASSWORD
) {}

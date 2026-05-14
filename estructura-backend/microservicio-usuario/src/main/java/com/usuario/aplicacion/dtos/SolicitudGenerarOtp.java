package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SolicitudGenerarOtp(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        String email,

                String telefono,

        @NotNull(message = "El tipo de canal es obligatorio")
                com.libreria.comun.enums.TipoVerificacion tipo,

        @NotNull(message = "El propósito es obligatorio")
                com.libreria.comun.enums.PropositoCodigo proposito
) {}

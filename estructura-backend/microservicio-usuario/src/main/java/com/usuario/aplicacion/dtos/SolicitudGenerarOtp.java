package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para solicitar al microservicio de mensajería la generación de un código.
 */
public record SolicitudGenerarOtp(
        @NotNull
        UUID usuarioId,
        @NotNull
        @Email
        String email,
        String telefono,
        @NotNull
        TipoVerificacion tipo, // Usamos Enums en ambos lados
        @NotNull
        PropositoCodigo proposito) {

}

package com.usuario.aplicacion.dtos.solicitudes;

import com.libreria.comun.enums.PropositoCodigo;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para verificar restricciones de límites de envío de OTP en ms-mensajeria.
 */
public record SolicitudVerificarLimite(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,
    
    @NotNull(message = "El propósito del código es obligatorio")
    PropositoCodigo proposito
) {}

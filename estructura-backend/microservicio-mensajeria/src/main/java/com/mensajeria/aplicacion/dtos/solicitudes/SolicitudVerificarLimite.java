package com.mensajeria.aplicacion.dtos.solicitudes;

import com.libreria.comun.enums.PropositoCodigo;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO mínimo para verificar las restricciones de límite de envío de OTP.
 */
public record SolicitudVerificarLimite(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,
    
    @NotNull(message = "El propósito del código es obligatorio")
    PropositoCodigo proposito
) {}

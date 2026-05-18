package com.mensajeria.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para la solicitud de verificación de límites de gasto.
 * Se utilizará para integraciones y validaciones.
 */
public record SolicitudVerificarLimite(
    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId,
    
    @NotNull(message = "El monto es obligatorio")
    BigDecimal monto
) {}

package com.suscripciones.aplicacion.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para la respuesta con los datos detallados de una suscripción.
 */
public record RespuestaSuscripcion(
        UUID id,
        UUID usuarioId,
        String nombre,
        BigDecimal monto,
        String estado,
        String metodoPago,
        java.time.LocalDate fechaInicio,
        java.time.LocalDate fechaVencimiento,
        java.time.LocalDate fechaUltimoPago,
        String tipoEstrategia,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}

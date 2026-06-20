package com.suscripciones.aplicacion.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para la respuesta con los datos de registro de pago de una suscripción.
 */
public record RespuestaPagoSuscripcion(
        UUID id,
        UUID suscripcionId,
        UUID transaccionId,
        BigDecimal monto,
        java.time.LocalDate fechaPago,
        String estado
) {
}

package com.suscripciones.aplicacion.dtos;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * DTO para la solicitud de registro manual de un pago de suscripción.
 */
public record SolicitudRegistrarPagoManual(
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero si se especifica")
        BigDecimal monto,

        String metodoPago,

        java.time.LocalDate fechaPago
) {
}

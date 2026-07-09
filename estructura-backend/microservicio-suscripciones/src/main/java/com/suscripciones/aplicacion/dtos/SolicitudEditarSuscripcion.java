package com.suscripciones.aplicacion.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO para la solicitud de edición de los datos de una suscripción.
 */
public record SolicitudEditarSuscripcion(
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero si se especifica")
        BigDecimal monto,

        @Size(max = 50, message = "El método de pago no debe superar los 50 caracteres")
        String metodoPago,

        java.util.UUID categoriaId,

        @Size(max = 30, message = "El tipo de estrategia no debe superar los 30 caracteres")
        String tipoEstrategia
) {
}

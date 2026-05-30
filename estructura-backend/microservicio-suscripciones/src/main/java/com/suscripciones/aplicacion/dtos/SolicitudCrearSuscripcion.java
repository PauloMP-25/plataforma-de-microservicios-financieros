package com.suscripciones.aplicacion.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para la solicitud de creación de una nueva suscripción de usuario.
 */
public record SolicitudCrearSuscripcion(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
        String nombre,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotBlank(message = "El método de pago es obligatorio")
        @Size(max = 50, message = "El método de pago no debe superar los 50 caracteres")
        String metodoPago,

        @Size(max = 30, message = "El tipo de estrategia no debe superar los 30 caracteres")
        String tipoEstrategia,

        java.time.LocalDate fechaInicio,

        java.time.LocalDate fechaVencimiento
) {
}

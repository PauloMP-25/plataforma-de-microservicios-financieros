package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO para la actualización de progreso de una meta de ahorro.
 */
public record SolicitudProgreso(
    @NotNull(message = "El monto actual es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido")
    BigDecimal montoActual
) {}

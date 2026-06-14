package com.cliente.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para crear o actualizar una meta de ahorro.
 */
public record SolicitudMetaAhorro(
        @NotBlank(message = "El nombre de la meta es obligatorio") @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres") String nombre,

        @NotNull(message = "El monto objetivo es obligatorio") @DecimalMin(value = "1.00", message = "El monto objetivo debe ser al menos S/ 1.00") @Digits(integer = 10, fraction = 2, message = "El monto objetivo tiene formato inválido") BigDecimal montoObjetivo,

        @DecimalMin(value = "0.00", message = "El monto actual no puede ser negativo") @Digits(integer = 10, fraction = 2, message = "El monto actual tiene formato inválido") BigDecimal montoActual,

        @Future(message = "La fecha límite debe ser una fecha futura") LocalDate fechaLimite,
        
        String proposito) {
}

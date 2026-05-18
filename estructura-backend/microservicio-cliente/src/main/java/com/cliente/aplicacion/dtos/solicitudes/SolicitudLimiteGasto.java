package com.cliente.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar un límite de gasto por categoría.
 */
public record SolicitudLimiteGasto(
                @NotNull(message = "El monto límite es obligatorio") @DecimalMin(value = "1.00", message = "El monto límite debe ser al menos S/ 1.00") @Digits(integer = 10, fraction = 2, message = "El monto límite tiene formato inválido")
                BigDecimal montoLimite,
                
                @Min(value = 1, message = "El porcentaje de alerta debe ser al menos 1%") @Max(value = 100, message = "El porcentaje de alerta no puede superar 100%")
                Integer porcentajeAlerta) {
}

package com.cliente.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para crear o actualizar un límite de gasto por categoría.
 *
 * @param nombre Nombre descriptivo del presupuesto.
 * @param montoLimite Monto máximo permitido.
 * @param porcentajeAlerta Porcentaje de consumo para notificar alertas.
 * @param fechaInicio Fecha de inicio del periodo.
 * @param fechaFin Fecha de fin del periodo.
 */
public record SolicitudLimiteGasto(
                @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
                String nombre,

                @NotNull(message = "El monto límite es obligatorio") @DecimalMin(value = "1.00", message = "El monto límite debe ser al menos S/ 1.00") @Digits(integer = 10, fraction = 2, message = "El monto límite tiene formato inválido")
                BigDecimal montoLimite,
                
                @Min(value = 1, message = "El porcentaje de alerta debe ser al menos 1%") @Max(value = 100, message = "El porcentaje de alerta no puede superar 100%")
                Integer porcentajeAlerta,

                LocalDate fechaInicio,
                LocalDate fechaFin) {
}

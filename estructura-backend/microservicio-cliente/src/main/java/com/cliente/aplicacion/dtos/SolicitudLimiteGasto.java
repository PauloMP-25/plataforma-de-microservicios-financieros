package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar un límite de gasto por categoría.
 *
 * La categoría acepta texto libre para ser compatible con los nombres que
 * devuelve el microservicio de clasificación Python.
 * Ejemplos: "Galletas", "Transporte", "Estudios", "Delivery", "Salud".
 */
@Data
public class SolicitudLimiteGasto {

    @NotBlank(message = "La categoría es obligatoria")
    @Size(min = 2, max = 100, message = "La categoría debe tener entre 2 y 100 caracteres")
    private String categoriaId;

    @NotNull(message = "El monto límite es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto límite debe ser al menos S/ 1.00")
    @Digits(integer = 10, fraction = 2, message = "El monto límite tiene formato inválido")
    private BigDecimal montoLimite;

    @Min(value = 1,   message = "El porcentaje de alerta debe ser al menos 1%")
    @Max(value = 100, message = "El porcentaje de alerta no puede superar 100%")
    private Integer porcentajeAlerta;
}

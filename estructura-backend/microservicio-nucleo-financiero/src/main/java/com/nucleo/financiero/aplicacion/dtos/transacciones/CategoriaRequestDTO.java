package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación y actualización de categorías financieras.
 *
 * @param nombre      Nombre de la categoría (obligatorio, max 100 caracteres).
 * @param descripcion Detalle de uso de la categoría.
 * @param icono       Nombre del icono (ej: 'shopping-cart').
 * @param tipo        Tipo de movimiento asociado (INGRESO/GASTO).
 */
public record CategoriaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        @Size(max = 50, message = "El icono no puede superar los 50 caracteres")
        String icono,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo
) {
}

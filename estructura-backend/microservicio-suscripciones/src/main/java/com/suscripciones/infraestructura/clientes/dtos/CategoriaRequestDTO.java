package com.suscripciones.infraestructura.clientes.dtos;

/**
 * DTO para la creación de categorías en ms-nucleo-financiero.
 */
public record CategoriaRequestDTO(
        String nombre,
        String descripcion,
        String icono,
        TipoMovimiento tipo
) {
}

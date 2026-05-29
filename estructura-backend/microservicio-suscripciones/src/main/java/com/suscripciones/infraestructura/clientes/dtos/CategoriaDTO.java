package com.suscripciones.infraestructura.clientes.dtos;

import java.util.UUID;

/**
 * DTO para la transferencia de información de categorías desde ms-nucleo-financiero.
 */
public record CategoriaDTO(
        UUID id,
        String nombre,
        String descripcion,
        String icono,
        TipoMovimiento tipo
) {
}

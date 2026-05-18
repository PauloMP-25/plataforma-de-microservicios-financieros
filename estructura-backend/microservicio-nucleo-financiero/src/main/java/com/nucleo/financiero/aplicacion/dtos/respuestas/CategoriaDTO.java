package com.nucleo.financiero.aplicacion.dtos.respuestas;

import lombok.Builder;
import java.util.UUID;

/**
 * DTO para la transferencia de información de categorías hacia la capa de presentación.
 *
 * @param id          Identificador único de la categoría.
 * @param nombre      Nombre descriptivo.
 * @param descripcion Detalle extendido de la categoría.
 * @param icono       Identificador de icono para el frontend.
 * @param tipo        Naturaleza de la categoría (INGRESO/GASTO).
 */
@Builder
public record CategoriaDTO(
        UUID id,
        String nombre,
        String descripcion,
        String icono,
        String tipo
) {
}

package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria;
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
    /**
     * Convierte una entidad de dominio {@link Categoria} en su representación DTO.
     * 
     * @param entidad Entidad de dominio.
     * @return DTO de categoría.
     */
    public static CategoriaDTO desde(Categoria entidad) {
        return CategoriaDTO.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .icono(entidad.getIcono())
                .tipo(entidad.getTipo().name())
                .build();
    }
}

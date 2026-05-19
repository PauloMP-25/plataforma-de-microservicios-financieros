package com.nucleo.financiero.aplicacion.mappers;

import com.nucleo.financiero.aplicacion.dtos.respuestas.CategoriaDTO;
import com.nucleo.financiero.dominio.entidades.Categoria;
import org.springframework.stereotype.Component;

/**
 * Mapper dedicado para la conversión de entidades {@link Categoria} a su DTO de salida {@link CategoriaDTO}.
 * <p>
 * Desacopla la lógica de presentación del dominio del negocio.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.0.0
 */
@Component
public class CategoriaMapper {

    /**
     * Convierte una entidad {@link Categoria} a DTO.
     * 
     * @param entidad Entidad de dominio.
     * @return DTO de respuesta.
     */
    public CategoriaDTO aDto(Categoria entidad) {
        if (entidad == null) {
            return null;
        }
        return CategoriaDTO.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .icono(entidad.getIcono())
                .tipo(entidad.getTipo().name())
                .build();
    }
}

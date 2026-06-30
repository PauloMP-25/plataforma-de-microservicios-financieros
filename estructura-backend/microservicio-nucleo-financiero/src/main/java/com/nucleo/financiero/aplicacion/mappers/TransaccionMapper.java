package com.nucleo.financiero.aplicacion.mappers;

import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import org.springframework.stereotype.Component;
import java.math.RoundingMode;

/**
 * Mapper dedicado para la conversión de entidades {@link Transaccion} a su DTO de salida {@link RespuestaTransaccion}.
 * <p>
 * Evita la mezcla de responsabilidades entre la presentación y el modelo.
 * </p>
 *
 * @version 1.0.0
 */
@Component
public class TransaccionMapper {

    /**
     * Convierte una entidad {@link Transaccion} a DTO.
     * 
     * @param entidad Entidad de dominio.
     * @return DTO de respuesta.
     */
    public RespuestaTransaccion aDto(Transaccion entidad) {
        if (entidad == null) {
            return null;
        }
        return RespuestaTransaccion.builder()
                .id(entidad.getId())
                .nombreCliente(entidad.getNombreCliente())
                .monto(entidad.getMonto().setScale(2, RoundingMode.HALF_UP))
                .tipo(entidad.getTipo().name())
                .categoria(entidad.getCategoria().getNombre())
                .categoriaIcono(entidad.getCategoria().getIcono())
                .fechaTransaccion(entidad.getFechaTransaccion())
                .metodoPago(entidad.getMetodoPago().name())
                .etiquetas(entidad.getEtiquetas())
                .descripcion(entidad.getDescripcion())
                .estado("Completed")
                .build();
    }
}

package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Transaccion;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida que representa una transacción persistida.
 *
 * @param id               ID único de la transacción.
 * @param monto            Monto de la operación.
 * @param tipo             Tipo (INGRESO/GASTO).
 * @param categoria        Nombre de la categoría asociada.
 * @param fechaTransaccion Fecha efectiva del movimiento.
 * @param metodoPago       Forma de pago utilizada.
 * @param etiquetas        Metadatos asociados.
 */
@Builder
public record RespuestaTransaccion(
        UUID id,
        BigDecimal monto,
        String tipo,
        String categoria,
        LocalDateTime fechaTransaccion,
        String metodoPago,
        String etiquetas
) {
    /**
     * Mapea una entidad {@link Transaccion} a este DTO de respuesta.
     * 
     * @param t Entidad de dominio.
     * @return DTO de respuesta transaccional.
     */
    public static RespuestaTransaccion desde(Transaccion t) {
        return RespuestaTransaccion.builder()
                .id(t.getId())
                .monto(t.getMonto())
                .tipo(t.getTipo().name())
                .categoria(t.getCategoria().getNombre())
                .fechaTransaccion(t.getFechaTransaccion())
                .metodoPago(t.getMetodoPago().name())
                .etiquetas(t.getEtiquetas())
                .build();
    }
}

package com.nucleo.financiero.aplicacion.dtos.respuestas;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida enriquecido que representa una transacción persistida.
 *
 * @param id               ID único de la transacción.
 * @param nombreCliente    Nombre o referencia del cliente.
 * @param monto            Monto de la operación.
 * @param tipo             Tipo (INGRESO/GASTO).
 * @param categoria        Nombre de la categoría asociada.
 * @param categoriaIcono   Icono de la categoría asociada para pintar en el frontend.
 * @param fechaTransaccion Fecha efectiva del movimiento.
 * @param metodoPago       Forma de pago utilizada.
 * @param etiquetas        Metadatos asociados.
 * @param descripcion      Detalle adicional de la transacción.
 * @param estado           Estado de la transacción para visualizaciones (ej: Completed).
 */
@Builder
public record RespuestaTransaccion(
        UUID id,
        String nombreCliente,
        BigDecimal monto,
        String tipo,
        String categoria,
        String categoriaIcono,
        LocalDateTime fechaTransaccion,
        String metodoPago,
        String etiquetas,
        String descripcion,
        String estado
) {
}

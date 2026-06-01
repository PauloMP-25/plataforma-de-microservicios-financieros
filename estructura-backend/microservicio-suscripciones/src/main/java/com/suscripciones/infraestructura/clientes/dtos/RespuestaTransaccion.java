package com.suscripciones.infraestructura.clientes.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa la respuesta al registrar una transacción en ms-nucleo-financiero.
 */
public record RespuestaTransaccion(
        UUID id,
        UUID usuarioId,
        String nombreCliente,
        BigDecimal monto,
        TipoMovimiento tipo,
        LocalDateTime fechaTransaccion,
        MetodoPago metodoPago,
        String etiquetas,
        String descripcion,
        LocalDateTime fechaRegistro
) {
}

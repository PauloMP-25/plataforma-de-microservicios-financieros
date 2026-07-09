package com.suscripciones.infraestructura.clientes.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para registrar una transacción en ms-nucleo-financiero.
 */
public record SolicitudTransaccion(
        UUID usuarioId,
        String nombreCliente,
        BigDecimal monto,
        TipoMovimiento tipo,
        UUID categoriaId,
        MetodoPago metodoPago,
        String etiquetas,
        String descripcion,
        java.time.LocalDateTime fechaTransaccion
) {
}

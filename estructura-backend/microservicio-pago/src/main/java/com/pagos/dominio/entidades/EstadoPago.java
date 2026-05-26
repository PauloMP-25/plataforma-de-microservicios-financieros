package com.pagos.dominio.entidades;

/**
 * Representa los posibles estados de una transacción de pago.
 */
public enum EstadoPago {
    PENDIENTE,
    COMPLETADO,
    FALLIDO,
    REEMBOLSADO
}

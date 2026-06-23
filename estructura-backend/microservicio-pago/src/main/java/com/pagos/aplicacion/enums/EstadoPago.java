package com.pagos.aplicacion.enums;

/**
 * Representa los posibles estados de una transacción de pago.
 */
public enum EstadoPago {
    PENDIENTE,
    AUTORIZADO,   // Preapproval autorizado por el usuario (Mercado Pago) — cobro recurrente pendiente
    COMPLETADO,
    FALLIDO,
    REEMBOLSADO,
    VENCIDO,
    EXPIRADO
}

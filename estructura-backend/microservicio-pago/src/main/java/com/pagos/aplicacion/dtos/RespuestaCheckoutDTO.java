package com.pagos.aplicacion.dtos;

import java.math.BigDecimal;

/**
 * Respuesta devuelta al frontend con la URL de Stripe Checkout y detalles de la transacción.
 */
public record RespuestaCheckoutDTO(
    String pagoId,
    String urlCheckout,
    String plan,
    BigDecimal monto,
    String moneda
) {}

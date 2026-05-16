package com.pagos.aplicacion.dtos;

import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.aplicacion.enums.PlanSuscripcion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proporciona información detallada sobre la suscripción actual del usuario.
 */
public record RespuestaSuscripcionDTO(
    PlanSuscripcion plan,
    EstadoPago estado,
    BigDecimal monto,
    String moneda,
    LocalDateTime fechaVencimiento,
    boolean activo
) {}

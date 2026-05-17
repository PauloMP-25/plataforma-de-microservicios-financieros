package com.pagos.aplicacion.dtos;

import com.pagos.aplicacion.enums.PlanSuscripcion;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para iniciar una sesión de pago con Stripe Checkout.
 * Se utiliza el patrón Record para inmutabilidad y concisión.
 */
public record SolicitudPagoDTO(
    @NotNull(message = "El plan es obligatorio")
    PlanSuscripcion plan
) {}

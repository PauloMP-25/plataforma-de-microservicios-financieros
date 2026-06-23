package com.pagos.aplicacion.dtos;

import com.pagos.aplicacion.enums.PlanSuscripcion;
import com.pagos.aplicacion.enums.ProveedorPago;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para iniciar una sesión de pago con cualquier pasarela soportada.
 *
 * <p>El campo {@code proveedor} es opcional. Si no se envía en el cuerpo de la
 * petición, se usará {@link ProveedorPago#STRIPE} por compatibilidad con el
 * frontend actual.</p>
 *
 * @author LUKA APP Team
 */
public record SolicitudPagoDTO(

    @NotNull(message = "El plan es obligatorio")
    PlanSuscripcion plan,

    /**
     * Proveedor de pago a usar. Por defecto {@code STRIPE} si no se especifica.
     * Valores aceptados: {@code STRIPE}, {@code MERCADOPAGO}.
     */
    ProveedorPago proveedor
) {
    /** Garantiza compatibilidad: si no se envía proveedor, devuelve STRIPE. */
    public ProveedorPago proveedorEfectivo() {
        return proveedor != null ? proveedor : ProveedorPago.STRIPE;
    }
}

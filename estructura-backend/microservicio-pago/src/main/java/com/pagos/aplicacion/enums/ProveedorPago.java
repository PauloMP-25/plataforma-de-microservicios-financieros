package com.pagos.aplicacion.enums;

/**
 * Enumeración de proveedores de pago soportados en LUKA APP.
 * <p>
 * Aplicar el Principio de Abierto/Cerrado (OCP): añadir un nuevo proveedor
 * solo requiere agregar un valor aquí y crear la implementación de
 * {@code IPasarelaPagoEstrategia} correspondiente.
 * </p>
 *
 * @author LUKA APP Team
 */
public enum ProveedorPago {

    /** Pasarela de pago Stripe (integración existente). */
    STRIPE,

    /** Pasarela de pago Mercado Pago — suscripciones recurrentes para el mercado peruano. */
    MERCADOPAGO
}

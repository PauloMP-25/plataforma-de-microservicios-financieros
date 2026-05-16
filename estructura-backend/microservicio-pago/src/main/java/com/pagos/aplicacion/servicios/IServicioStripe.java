package com.pagos.aplicacion.servicios;

import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;

import java.util.UUID;

/**
 * Interfaz para el servicio de integración con Stripe.
 */
public interface IServicioStripe {

    /**
     * Crea una sesión de Stripe Checkout y registra el intento de pago.
     * 
     * @param solicitud DTO con los datos del plan.
     * @param usuarioId ID del usuario autenticado.
     * @param emailUsuario Email del usuario para la sesión de Stripe.
     * @return DTO con la URL de redirección.
     */
    RespuestaCheckoutDTO crearSesionCheckout(SolicitudPagoDTO solicitud, UUID usuarioId, String emailUsuario);

    /**
     * Recupera la información de la suscripción actual del usuario.
     * 
     * @param usuarioId ID del usuario.
     * @return DTO con detalles del plan y vencimiento.
     */
    RespuestaSuscripcionDTO obtenerEstadoSuscripcion(UUID usuarioId);
}

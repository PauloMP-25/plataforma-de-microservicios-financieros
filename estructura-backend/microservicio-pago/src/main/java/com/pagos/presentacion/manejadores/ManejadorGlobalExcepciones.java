package com.pagos.presentacion.manejadores;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.respuesta.ResultadoApi;
import com.stripe.exception.CardException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.pagos.infraestructura.mensajeria.PublicadorAuditoriaPagosImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pagos.infraestructura.excepciones.ExcepcionMercadoPago;
import com.pagos.infraestructura.excepciones.ExcepcionFirmaWebhookInvalida;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * Manejador global de excepciones para el microservicio de pagos.
 * Extiende la funcionalidad base de la librería común para centralizar el control de errores.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    private final PublicadorAuditoriaPagosImpl publicadorAuditoria;

    /**
     * Captura y traduce errores específicos provenientes del SDK de Stripe.
     * Evita que errores de la pasarela se muestren como errores 500 genéricos.
     */
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ResultadoApi<?>> manejarStripeException(StripeException ex, HttpServletRequest req) {
        log.error("[STRIPE-ERROR] Ocurrió una excepción en la pasarela: {}", ex.getMessage());

        UUID usuarioId = UtilidadSeguridad.obtenerUsuarioId();

        // Auditar el fallo en el sistema de auditoría centralizado
        publicadorAuditoria.auditarEventoSeguridad(
                usuarioId,
                "STRIPE_ERROR",
                "Fallo en pasarela: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                EstadoEvento.FALLO,
                req
        );

        if (ex instanceof CardException cardEx) {
            // Error con la tarjeta del usuario (ej: fondos insuficientes, expirada)
            return ResponseEntity.badRequest()
                    .body(ResultadoApi.falla(CodigoError.PAGO_RECHAZADO, cardEx.getMessage(), req.getRequestURI()));
        }

        if (ex instanceof RateLimitException) {
            // Demasiadas peticiones al API de Stripe
            return ResponseEntity.status(429)
                    .body(ResultadoApi.falla(CodigoError.LIMITE_DIARIO_EXCEDIDO, "La pasarela de pagos está temporalmente saturada. Intente más tarde.", req.getRequestURI()));
        }

        // Error general de comunicación o configuración con Stripe
        return ResponseEntity.status(502)
                .body(ResultadoApi.falla(CodigoError.ERROR_SERVICIO_EXTERNO, "No se pudo completar la operación con el proveedor de pagos.", req.getRequestURI()));
    }

    /**
     * Captura errores de comunicación con la API de Mercado Pago.
     * Mapea al código HTTP 502 (Bad Gateway) usando CodigoError.ERROR_SERVICIO_EXTERNO.
     *
     * @param ex  Excepción de Mercado Pago.
     * @param req Petición HTTP que generó el error.
     * @return Respuesta estandarizada con ResultadoApi de libreria-comun.
     */
    @ExceptionHandler(ExcepcionMercadoPago.class)
    public ResponseEntity<ResultadoApi<?>> manejarMercadoPagoException(
            ExcepcionMercadoPago ex, HttpServletRequest req) {
        log.error("[MERCADOPAGO-ERROR] Fallo en operación con pasarela: {}", ex.getMessage());
        return ResponseEntity.status(CodigoError.ERROR_SERVICIO_EXTERNO.getStatus())
                .body(ResultadoApi.falla(CodigoError.ERROR_SERVICIO_EXTERNO,
                        "No se pudo completar la operación con Mercado Pago. Intente más tarde.",
                        req.getRequestURI()));
    }

    /**
     * Captura intentos de webhook con firma HMAC-SHA256 inválida.
     * Mapea al código HTTP 403 (Forbidden) usando CodigoError.ACCESO_DENEGADO.
     *
     * @param ex  Excepción de firma inválida.
     * @param req Petición HTTP del webhook.
     * @return Respuesta estandarizada con ResultadoApi de libreria-comun.
     */
    @ExceptionHandler(ExcepcionFirmaWebhookInvalida.class)
    public ResponseEntity<ResultadoApi<?>> manejarFirmaWebhookInvalida(
            ExcepcionFirmaWebhookInvalida ex, HttpServletRequest req) {
        log.error("[WEBHOOK-MP-SEGURIDAD] Firma inválida rechazada desde: {}", req.getRemoteAddr());
        return ResponseEntity.status(CodigoError.ACCESO_DENEGADO.getStatus())
                .body(ResultadoApi.falla(CodigoError.ACCESO_DENEGADO,
                        ex.getMessage(),
                        req.getRequestURI()));
    }
}

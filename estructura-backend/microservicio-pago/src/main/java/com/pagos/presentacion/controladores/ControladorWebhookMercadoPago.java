package com.pagos.presentacion.controladores;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.pagos.aplicacion.servicios.MercadoPagoEstrategiaImpl;
import com.pagos.infraestructura.excepciones.ExcepcionFirmaWebhookInvalida;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receptor de notificaciones IPN/Webhooks de Mercado Pago.
 *
 * <p><strong>CRÍTICO DE SEGURIDAD:</strong> Este endpoint NO lleva autenticación JWT,
 * ya que Mercado Pago no envía tokens de usuario. La seguridad se garantiza exclusivamente
 * mediante la validación de la firma HMAC-SHA256 del header {@code x-signature}.</p>
 *
 * <p>El endpoint está registrado como ruta pública en el filtro JWT del API Gateway
 * ({@code app.security.rutas-publicas}), pero la validación criptográfica interna
 * garantiza que solo Mercado Pago pueda generar notificaciones válidas.</p>
 *
 * <p>El endpoint retorna {@code 200 OK} en cuanto confirma la recepción (incluso si el
 * estado no requiere acción), para evitar reintentos innecesarios de Mercado Pago.</p>
 *
 * @see MercadoPagoEstrategiaImpl#procesarNotificacion
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pagos/webhook/mercadopago")
@RequiredArgsConstructor
public class ControladorWebhookMercadoPago {

    private final MercadoPagoEstrategiaImpl estrategiaMercadoPago;

    /**
     * Recibe, valida y procesa las notificaciones de Mercado Pago.
     *
     * <p>Flujo de procesamiento:</p>
     * <ol>
     *   <li>Extrae los headers de seguridad {@code x-signature} y {@code x-request-id}.</li>
     *   <li>Delega la validación de firma y el procesamiento a {@link MercadoPagoEstrategiaImpl}.</li>
     *   <li>Retorna {@code 200 OK} inmediatamente para confirmar la recepción a Mercado Pago.</li>
     * </ol>
     *
     * @param payloadCrudo Cuerpo crudo del POST enviado por Mercado Pago.
     * @param firma        Header {@code x-signature} con el timestamp y el hash HMAC-SHA256.
     * @param requestId    Header {@code x-request-id} identificador único de la notificación.
     * @param request      Petición HTTP (para auditoría de seguridad).
     * @return {@code 200 OK} si la notificación es válida y procesada correctamente.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<String>> recibirNotificacion(
            @RequestBody String payloadCrudo,
            @RequestHeader(value = "x-signature", required = false) String firma,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            HttpServletRequest request) {

        log.debug("[WEBHOOK-MP] Notificación recibida. RequestId: {} | IP: {}",
                requestId, request.getRemoteAddr());

        // Validar presencia de headers críticos de seguridad
        if (firma == null || firma.isBlank()) {
            log.error("[WEBHOOK-MP-SEGURIDAD] Header x-signature ausente. Posible ataque.");
            return ResponseEntity.badRequest()
                    .body(ResultadoApi.falla(CodigoError.ACCESO_DENEGADO,
                            "Header x-signature requerido.", "/webhook/mercadopago"));
        }

        if (requestId == null || requestId.isBlank()) {
            log.error("[WEBHOOK-MP-SEGURIDAD] Header x-request-id ausente.");
            return ResponseEntity.badRequest()
                    .body(ResultadoApi.falla(CodigoError.SOLICITUD_INCORRECTA,
                            "Header x-request-id requerido.", "/webhook/mercadopago"));
        }

        try {
            // Delegar validación de firma, idempotencia y procesamiento a la estrategia
            estrategiaMercadoPago.procesarNotificacion(payloadCrudo, firma, requestId, request);

            return ResponseEntity.ok(
                    ResultadoApi.exito("Notificación procesada correctamente.", "Webhook MP OK"));

        } catch (ExcepcionFirmaWebhookInvalida e) {
            log.error("[WEBHOOK-MP-SEGURIDAD] Firma inválida rechazada: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResultadoApi.falla(CodigoError.ACCESO_DENEGADO,
                            e.getMessage(), "/webhook/mercadopago"));

        } catch (Exception e) {
            log.error("[WEBHOOK-MP-ERROR] Error interno al procesar notificación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResultadoApi.falla(CodigoError.ERROR_INTERNO,
                            "Error al procesar la notificación de Mercado Pago.",
                            "/webhook/mercadopago"));
        }
    }
}

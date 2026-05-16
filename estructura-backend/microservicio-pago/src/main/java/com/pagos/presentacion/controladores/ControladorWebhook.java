package com.pagos.presentacion.controladores;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.pagos.aplicacion.servicios.IServicioWebhook;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receptor de eventos de Stripe (Webhooks).
 * CRÍTICO: Este endpoint NO lleva autenticación JWT ya que Stripe no envía tokens.
 * La seguridad se garantiza mediante la verificación de la firma Stripe-Signature.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pagos/webhook")
@RequiredArgsConstructor
public class ControladorWebhook {

    private final IServicioWebhook servicioWebhook;

    @Value("${stripe.webhook-secret}")
    private String secretoWebhook;

    /**
     * Recibe, valida y delega el procesamiento de eventos de Stripe.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<String>> recibirEvento(
            @RequestBody String payloadCrudo,
            @RequestHeader("Stripe-Signature") String firmaStripe) {

        log.debug("[WEBHOOK] Petición recibida en webhook de Stripe");

        Event evento;
        try {
            // Validación de la autenticidad del payload usando el secreto del webhook
            evento = Webhook.constructEvent(payloadCrudo, firmaStripe, secretoWebhook);
        } catch (SignatureVerificationException e) {
            log.error("[WEBHOOK] Firma inválida detectada: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResultadoApi.falla(CodigoError.ACCESO_DENEGADO, "Firma del webhook inválida", "/webhook"));
        } catch (Exception e) {
            log.error("[WEBHOOK] Error procesando payload: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ResultadoApi.falla(CodigoError.ERROR_INTERNO, "Error interno al procesar webhook", "/webhook"));
        }

        log.info("[WEBHOOK] Evento verificado: {} | Tipo: {}", evento.getId(), evento.getType());

        // Procesamiento asíncrono o transaccional
        servicioWebhook.procesarEvento(evento);

        // Confirmar recepción a Stripe (debe ser rápido para evitar reintentos)
        return ResponseEntity.ok(ResultadoApi.exito("Evento recibido y procesado correctamente", "Webhook OK"));
    }
}

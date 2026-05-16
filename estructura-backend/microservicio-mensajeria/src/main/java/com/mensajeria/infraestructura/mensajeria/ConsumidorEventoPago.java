package com.mensajeria.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import com.mensajeria.aplicacion.servicios.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de pago para el sistema de mensajería.
 * Envía el comprobante de pago y bienvenida al usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoPago {

    private final NotificacionService notificacionService;

    /**
     * Al detectar un pago exitoso, envía un correo de confirmación.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS)
    public void enviarComprobantePago(EventoPagoExitosoDTO evento) {
        log.info("[MENSAJERIA-CONSUMER] Enviando comprobante a: {}", evento.emailUsuario());

        String asunto = "¡Bienvenido al plan " + evento.planNuevo() + "! — LUKA APP";
        String cuerpo = String.format(
            "<h1>¡Gracias por tu suscripción!</h1>" +
            "<p>Hola,</p>" +
            "<p>Tu pago ha sido procesado con éxito. Ahora tienes acceso a los beneficios del plan <strong>%s</strong>.</p>" +
            "<ul>" +
            "<li>Monto: %s %s</li>" +
            "<li>Vencimiento: %s</li>" +
            "</ul>" +
            "<p>¡Disfruta de LUKA APP!</p>",
            evento.planNuevo(), evento.monto(), evento.moneda(), evento.fechaFinPlan().toLocalDate()
        );

        try {
            notificacionService.enviarEmailAdministrador(
                evento.emailUsuario(),
                asunto,
                cuerpo,
                true
            );
            log.info("[MENSAJERIA-SUCCESS] Comprobante enviado exitosamente a: {}", evento.emailUsuario());
        } catch (Exception e) {
            log.error("[MENSAJERIA-ERROR] No se pudo enviar el comprobante: {}", e.getMessage());
            // No relanzamos si es un error de envío de mail para no bloquear la cola de pagos, 
            // pero se debería loguear para seguimiento.
        }
    }
}

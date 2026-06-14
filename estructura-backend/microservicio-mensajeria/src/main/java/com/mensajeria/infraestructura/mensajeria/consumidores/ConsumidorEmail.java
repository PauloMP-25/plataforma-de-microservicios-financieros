package com.mensajeria.infraestructura.mensajeria.consumidores;

import com.mensajeria.infraestructura.mensajeria.ConfiguracionRabbitMQ;
import com.libreria.comun.dtos.SolicitudEmailDTO;
import com.mensajeria.aplicacion.servicios.canales.NotificacionAdminService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola de correos electrónicos.
 * Procesa peticiones genéricas de envío de email desde cualquier microservicio.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorEmail {

    private final NotificacionAdminService notificacionService;

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_EMAIL_ENVIAR)
    public void procesarEnvioEmail(
            SolicitudEmailDTO solicitud,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        log.info("[AMQP] Recibida solicitud de email para: {}", solicitud.destinatario());
        
        try {
            notificacionService.enviarEmailAdministrador(
                solicitud.destinatario(),
                solicitud.asunto(),
                solicitud.cuerpo(),
                solicitud.esHtml()
            );
            log.info("[AMQP] Alerta administrativa enviada a: {}", solicitud.destinatario());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[AMQP] Error al procesar email para '{}' asunto='{}': {}", solicitud.destinatario(), solicitud.asunto(), e.getMessage());
            // Rechazamos el mensaje enviándolo a la DLQ (requeue = false) para evitar re-entregas infinitas
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

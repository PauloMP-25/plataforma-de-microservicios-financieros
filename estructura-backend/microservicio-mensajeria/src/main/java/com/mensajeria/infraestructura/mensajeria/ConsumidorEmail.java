package com.mensajeria.infraestructura.mensajeria;

import com.libreria.comun.dtos.SolicitudEmailDTO;
import com.mensajeria.aplicacion.servicios.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola de correos electrónicos.
 * Procesa peticiones genéricas de envío de email desde cualquier microservicio.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorEmail {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_EMAIL_ENVIAR)
    public void procesarEnvioEmail(SolicitudEmailDTO solicitud) {
        log.info("[AMQP] Recibida solicitud de email para: {}", solicitud.destinatario());
        
        try {
            notificacionService.enviarEmailAdministrador(
                solicitud.destinatario(),
                solicitud.asunto(),
                solicitud.cuerpo(),
                solicitud.esHtml()
            );
            log.info("[AMQP] Alerta administrativa enviada a: {}", solicitud.destinatario());
        } catch (Exception e) {
            log.error("[AMQP] Error al procesar email: {}", e.getMessage());
            // El mensaje se irá a la DLQ si falla (configurado en RabbitConfig)
            throw e;
        }
    }
}

package com.mensajeria.infraestructura.mensajeria.consumidores;

import com.mensajeria.infraestructura.mensajeria.ConfiguracionRabbitMQ;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorOtp {

    private final IMensajeriaService mensajeriaService;

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_OTP_GENERAR)
    public void procesarSolicitudOtp(
            SolicitudGenerarCodigo solicitud,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        log.info("[RABBITMQ] Solicitud de OTP recibida para usuario: {} - Propósito: {}", 
                 solicitud.usuarioId(), solicitud.proposito());
        
        try {
            // Reutilizamos tu lógica de negocio existente
            mensajeriaService.generarYEnviarCodigo(solicitud);
            log.debug("[RABBITMQ] OTP procesado y enviado con éxito.");
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[RABBITMQ] Error procesando solicitud de OTP: {}", e.getMessage());
            // Rechazamos el mensaje enviándolo a la DLQ (requeue = false) para evitar re-entregas infinitas
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

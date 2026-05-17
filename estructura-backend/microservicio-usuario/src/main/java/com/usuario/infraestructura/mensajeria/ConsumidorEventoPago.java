package com.usuario.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import com.usuario.aplicacion.puertos.IServicioRol;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Escucha eventos de pago exitoso provenientes de ms-pagos.
 * Actualiza el plan y la vigencia de la suscripción del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoPago {

    private final IServicioRol servicioRol;

    /**
     * Procesa el mensaje de pago exitoso con acuse de recibo (ACK) manual.
     * Evita la re-entrega infinita si ocurre un error inesperado al procesar la actualización.
     *
     * @param evento  DTO del evento de pago exitoso.
     * @param channel Canal de comunicación AMQP.
     * @param tag     Etiqueta de entrega del mensaje.
     * @throws IOException Si ocurre un error de comunicación AMQP.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS)
    public void manejarPagoExitoso(
            EventoPagoExitosoDTO evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        
        log.info("[RABBITMQ-CONSUMER] Recibido pago exitoso para usuario: {}", evento.usuarioId());
        
        try {
            servicioRol.actualizarPlanUsuario(
                evento.usuarioId(),
                evento.planNuevo(),
                evento.fechaFinPlan()
            );
            
            // Confirmamos la recepción y procesamiento exitoso (ACK manual)
            channel.basicAck(tag, false);
            log.info("[RABBITMQ-CONSUMER] ACK enviado exitosamente para deliveryTag: {}", tag);
            
        } catch (Exception e) {
            log.error("[RABBITMQ-ERROR] Error al procesar pago exitoso para usuario {}: {}", 
                evento.usuarioId(), e.getMessage());
            
            // Rechazamos el mensaje (NACK manual) con requeue = false para enviarlo directamente a la DLQ
            channel.basicNack(tag, false, false);
            log.warn("[RABBITMQ-CONSUMER] NACK enviado para deliveryTag: {}, mensaje enviado a DLQ.", tag);
        }
    }
}

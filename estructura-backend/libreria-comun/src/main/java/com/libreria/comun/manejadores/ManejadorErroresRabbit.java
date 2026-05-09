package com.luka.commons.manejadores;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

/**
 * Manejador global de errores para los consumidores de RabbitMQ en LUKA APP.
 * <p>
 * Este componente intercepta excepciones lanzadas dentro de los métodos
 * anotados con {@code @RabbitListener}. Permite centralizar el log de errores
 * de mensajería y decidir si el mensaje debe ser reintentado, descartado o
 * enviado a una Dead Letter Queue.
 * </p>
 *
 * @author Paulo Moron
 */
@Slf4j
@Component
public class ManejadorErroresRabbit implements RabbitListenerErrorHandler {

    /**
     * Intercepta y procesa los errores ocurridos durante la ejecución de un
     * listener.
     *
     * @param msg El mensaje original de RabbitMQ (formato
     * org.springframework.amqp.core).
     * @param channel El canal de RabbitMQ para operaciones manuales (ack/nack).
     * @param message El mensaje convertido al formato de mensajería de Spring.
     * @param exception La excepción capturada durante el procesamiento.
     * @return null para indicar que la excepción ha sido consumida, o lanza una
     * excepción para reintento.
     * @throws Exception Si ocurre un error durante el manejo del fallo.
     */
    @Override
    public Object handleError(org.springframework.amqp.core.Message msg,
            com.rabbitmq.client.Channel channel,
            org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception) throws Exception {

        Throwable causa = exception.getCause();
        String routingKey = (String) message.getHeaders().get(org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY);

        // Si es un error de código (NullPointer, etc), no reintentamos para no bloquear la cola
        if (causa instanceof IllegalArgumentException || causa instanceof NullPointerException) {
            log.error("[LUKA-DLQ] Error fatal en {}. Enviando a DLQ: {}", routingKey, causa.getMessage());
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Error de lógica, mensaje descartado", causa);
        }

        log.error("[LUKA-REINTENTO] Error transitorio en {}. Reencolando...", routingKey);
        throw exception; // Esto dispara el reintento automático de Spring
    }
}

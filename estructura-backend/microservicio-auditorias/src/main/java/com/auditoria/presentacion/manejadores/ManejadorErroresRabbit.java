package com.auditoria.presentacion.manejadores;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("auditoriaErrorHandler")
@Slf4j
public class ManejadorErroresRabbit implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(org.springframework.amqp.core.Message msg, 
            Channel chnl, Message<?> message, 
            ListenerExecutionFailedException exception) throws Exception {
        // 1. Obtenemos la causa real del error
        Throwable causa = exception.getCause();

        // 2. En Spring Messaging, el Routing Key está en los Headers, no en Properties
        String routingKey = (String) message.getHeaders().get(AmqpHeaders.RECEIVED_ROUTING_KEY);

        if (causa instanceof IllegalArgumentException || causa instanceof NullPointerException) {
            log.error("[DLQ-AUTOMATICO] Error de validación en {}. Enviando a DLQ: {}",
                    routingKey, causa.getMessage());

            // Lanzar esta excepción le dice a RabbitMQ: "No reintentar, va a la DLQ"
            throw new AmqpRejectAndDontRequeueException("Datos inválidos, no reintentar", causa);
        }

        log.error("[REINTENTO] Error transitorio en {}. Reencolando mensaje: {}",
                routingKey, causa != null ? causa.getMessage() : "Error desconocido");

        // Volver a lanzar la excepción original activa el reintento automático de Spring
        throw exception;
    }
}

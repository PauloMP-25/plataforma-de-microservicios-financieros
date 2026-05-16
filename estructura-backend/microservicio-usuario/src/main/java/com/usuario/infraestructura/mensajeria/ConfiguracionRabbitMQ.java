package com.usuario.infraestructura.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la topología de RabbitMQ para el microservicio de usuario.
 * <p>
 * Los beans de infraestructura (ConnectionFactory, RabbitTemplate,
 * MessageConverter)
 * son provistos automáticamente por {@code libreria-comun}.
 * </p>
 */
@Configuration
public class ConfiguracionRabbitMQ {

    // Nombres de Exchange y Colas para Auditoría
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";
    public static final String COLA_AUDITORIA = "cola.auditoria";
    public static final String ROUTING_KEY_AUDITORIA = "cola.auditoria";

    /**
     * Define la cola de auditoría de forma duradera.
     */
    @Bean
    public Queue colaAuditoria() {
        return QueueBuilder.durable(COLA_AUDITORIA).build();
    }

    /**
     * Define el exchange de auditoría de tipo Direct.
     */
    @Bean
    public DirectExchange exchangeAuditoria() {
        return new DirectExchange(EXCHANGE_AUDITORIA);
    }

    /**
     * Realiza el enlace entre la cola y el exchange de auditoría.
     * Se agrega @Qualifier para evitar ambigüedad con otros beans de tipo Queue o
     * DirectExchange.
     */
    @Bean
    public Binding bindingAuditoria(
            @Qualifier("colaAuditoria") Queue colaAuditoria,
            @Qualifier("exchangeAuditoria") DirectExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAuditoria)
                .to(exchangeAuditoria)
                .with(ROUTING_KEY_AUDITORIA);
    }
}

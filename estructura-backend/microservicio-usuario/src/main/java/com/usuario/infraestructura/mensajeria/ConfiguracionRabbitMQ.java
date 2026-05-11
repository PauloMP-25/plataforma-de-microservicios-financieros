package com.usuario.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.ConfiguracionRabbitBase;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el microservicio de usuario.
 * Extiende de ConfiguracionRabbitBase para heredar la infraestructura de conexión y serialización JSON.
 */
@Configuration
public class ConfiguracionRabbitMQ extends ConfiguracionRabbitBase {

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
     */
    @Bean
    public Binding bindingAuditoria(Queue colaAuditoria, DirectExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAuditoria)
                .to(exchangeAuditoria)
                .with(ROUTING_KEY_AUDITORIA);
    }
}

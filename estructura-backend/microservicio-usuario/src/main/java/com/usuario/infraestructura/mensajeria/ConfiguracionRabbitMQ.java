package com.usuario.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la topología de RabbitMQ para el microservicio de usuario.
 * <p>
 * Los beans de infraestructura (ConnectionFactory, RabbitTemplate,
 * MessageConverter) son provistos automáticamente por {@code libreria-comun}.
 * Utiliza centralizadamente las colas, exchanges y routing keys de la librería
 * común,
 * configurando exchanges de tipo Topic para máxima flexibilidad y
 * escalabilidad.
 * </p>
 */
@Configuration
public class ConfiguracionRabbitMQ {
    // Forzar despliegue en Render para cargar la nueva libreria-comun

    /**
     * Define la cola de auditoría de accesos de forma duradera enlazada a su DLX.
     */
    @Bean
    public Queue colaAuditoria() {
        return QueueBuilder.durable(NombresCola.AUDITORIA_ACCESOS)
                .withArgument("x-dead-letter-exchange", NombresExchange.AUDITORIA_DLX)
                .withArgument("x-dead-letter-routing-key", RoutingKeys.DLQ_AUDITORIA_ACCESO)
                .build();
    }

    /**
     * Define el exchange de auditoría de tipo Topic.
     */
    @Bean
    public TopicExchange exchangeAuditoria() {
        return new TopicExchange(NombresExchange.AUDITORIA);
    }

    /**
     * Realiza el enlace entre la cola y el exchange de auditoría.
     * Se agrega @Qualifier para evitar ambigüedad con otros beans de tipo Queue o
     * DirectExchange.
     * @param colaAuditoria
     * @param exchangeAuditoria
     * @return 
     */
    @Bean
    public Binding bindingAuditoria(
            @Qualifier("colaAuditoria") Queue colaAuditoria,
            @Qualifier("exchangeAuditoria") TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAuditoria)
                .to(exchangeAuditoria)
                .with(RoutingKeys.AUDITORIA_ACCESO_ALL);
    }

    /**
     * Define la cola de Dead Letter Queue (DLQ) para auditoría de accesos.
     */
    @Bean
    public Queue colaAuditoriaDlq() {
        return QueueBuilder.durable(NombresCola.AUDITORIA_ACCESOS_DLQ).build();
    }

    /**
     * Define el Dead Letter Exchange (DLX) de tipo Direct para auditoría para coincidir con RabbitMQ.
     */
    @Bean
    public DirectExchange dlxAuditoria() {
        return new DirectExchange(NombresExchange.AUDITORIA_DLX);
    }

    /**
     * Realiza el enlace entre la cola DLQ y el Dead Letter Exchange usando patrones
     * Direct.
     */
    @Bean
    public Binding bindingAuditoriaDlq(
            @Qualifier("colaAuditoriaDlq") Queue colaAuditoriaDlq,
            @Qualifier("dlxAuditoria") DirectExchange dlxAuditoria) {
        return BindingBuilder
                .bind(colaAuditoriaDlq)
                .to(dlxAuditoria)
                .with(RoutingKeys.DLQ_AUDITORIA_ACCESO);
    }

    /**
     * Define el exchange de pagos.
     */
    @Bean
    public TopicExchange exchangePagos() {
        return new TopicExchange(NombresExchange.PAGOS, true, false);
    }

    /**
     * Define la cola de pagos exitosos específica de usuario.
     */
    @Bean
    public Queue queuePagosExitososUsuario() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_USUARIO).build();
    }

    /**
     * Realiza el enlace entre la cola de pagos de usuario y el exchange de pagos.
     */
    @Bean
    public Binding bindingPagosUsuario(
            @Qualifier("queuePagosExitososUsuario") Queue queuePagosExitososUsuario,
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
                .bind(queuePagosExitososUsuario)
                .to(exchangePagos)
                .with(RoutingKeys.PAGO_EXITOSO);
    }
}

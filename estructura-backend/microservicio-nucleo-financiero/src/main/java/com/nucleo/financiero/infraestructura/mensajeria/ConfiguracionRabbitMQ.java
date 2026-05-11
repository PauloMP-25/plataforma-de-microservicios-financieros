package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.ConfiguracionRabbitBase;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura de RabbitMQ para el Núcleo Financiero.
 * <p>
 * Centraliza la definición de exchanges, colas y bindings del módulo, asegurando
 * la implementación de estrategias de resiliencia mediante Dead Letter Queues (DLQ).
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.1
 */
@Configuration
public class ConfiguracionRabbitMQ extends ConfiguracionRabbitBase {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    @Bean
    public TopicExchange exchangeIA() {
        return new TopicExchange(NombresExchange.IA);
    }

    @Bean
    public TopicExchange exchangeAuditoria() {
        return new TopicExchange(NombresExchange.AUDITORIA);
    }

    /**
     * Topic Exchange centralizado para mensajes fallidos (Dead Letter Exchange).
     */
    @Bean
    public TopicExchange exchangeDLQ() {
        return new TopicExchange(NombresExchange.AUDITORIA_DLX);
    }

    // =========================================================================
    // COLAS PRINCIPALES (Integradas con la Librería)
    // =========================================================================

    @Bean
    public Queue colaIAProcesamiento() {
        return QueueBuilder.durable(NombresCola.IA_PROCESAMIENTO)
                .withArgument("x-dead-letter-exchange", NombresExchange.AUDITORIA_DLX)
                .withArgument("x-dead-letter-routing-key", NombresCola.IA_PROCESAMIENTO + ".dlq")
                .build();
    }

    @Bean
    public Queue colaIASincronizacionContexto() {
        return QueueBuilder.durable(NombresCola.IA_SINCRONIZACION_CONTEXTO).build();
    }

    @Bean
    public Queue colaIASincronizacionError() {
        return QueueBuilder.durable(NombresCola.IA_SINCRONIZACION_ERROR).build();
    }

    // =========================================================================
    // COLAS DLQ (Resiliencia)
    // =========================================================================

    @Bean
    public Queue colaIADLQ() {
        return QueueBuilder.durable(NombresCola.IA_PROCESAMIENTO + ".dlq").build();
    }

    // =========================================================================
    // BINDINGS
    // =========================================================================

    @Bean
    public Binding bindingIAAnalisis(Queue colaIAProcesamiento, TopicExchange exchangeIA) {
        return BindingBuilder.bind(colaIAProcesamiento)
                .to(exchangeIA)
                .with(RoutingKeys.IA_ANALISIS_SOLICITAR);
    }

    @Bean
    public Binding bindingIADLQ(Queue colaIADLQ, TopicExchange exchangeDLQ) {
        return BindingBuilder.bind(colaIADLQ)
                .to(exchangeDLQ)
                .with(NombresCola.IA_PROCESAMIENTO + ".dlq");
    }
}

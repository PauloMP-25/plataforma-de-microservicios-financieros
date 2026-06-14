package com.nucleo.financiero.infraestructura.configuracion;

import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura de RabbitMQ para el Núcleo Financiero.
 * <p>
 * Centraliza la definición de exchanges, colas y bindings del módulo,
 * asegurando la implementación de estrategias de resiliencia mediante Dead Letter Queues (DLQ).
 * Utiliza la infraestructura de conexión y serialización JSON provista por ConfiguracionRabbitBase.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.3.0
 */
@Configuration
public class ConfiguracionRabbitMQ {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    @Bean
    public DirectExchange exchangeIA() {
        return new DirectExchange(NombresExchange.IA);
    }

    @Bean
    public TopicExchange exchangeAuditoria() {
        return new TopicExchange(NombresExchange.AUDITORIA);
    }

    /**
     * Direct Exchange centralizado para mensajes fallidos (Dead Letter Exchange).
     */
    @Bean
    public DirectExchange exchangeDLQ() {
        return new DirectExchange(NombresExchange.AUDITORIA_DLX);
    }

    // =========================================================================
    // COLAS PRINCIPALES (Integradas con la Librería)
    // =========================================================================

    @Bean
    public Queue colaIAProcesamiento() {
        return QueueBuilder.durable(NombresCola.IA_PROCESAMIENTO)
                .withArgument("x-dead-letter-exchange", "exchange.ia.dlx")
                .withArgument("x-dead-letter-routing-key", NombresCola.IA_PROCESAMIENTO)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    @Bean
    public Queue colaIASincronizacionContexto() {
        return QueueBuilder.durable(NombresCola.IA_SINCRONIZACION_CONTEXTO)
                .withArgument("x-dead-letter-exchange", NombresExchange.CLIENTE_ACTUALIZACIONES_DLX)
                .withArgument("x-dead-letter-routing-key", NombresCola.IA_SINCRONIZACION_ERROR)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    @Bean
    public Queue colaIASincronizacionError() {
        return QueueBuilder.durable(NombresCola.IA_SINCRONIZACION_ERROR).build();
    }

    @Bean
    public Queue colaPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_FINANCIERO).build();
    }

    // =========================================================================
    // COLAS DLQ (Resiliencia)
    // =========================================================================

    @Bean
    public Queue colaIADLQ() {
        return QueueBuilder.durable(NombresCola.IA_PROCESAMIENTO_DLQ).build();
    }

    // =========================================================================
    // BINDINGS
    // =========================================================================

    @Bean
    public Binding bindingIAAnalisis(Queue colaIAProcesamiento, DirectExchange exchangeIA) {
        return BindingBuilder.bind(colaIAProcesamiento)
                .to(exchangeIA)
                .with(RoutingKeys.IA_ANALISIS_SOLICITAR);
    }

    @Bean
    public Binding bindingIADLQ(Queue colaIADLQ, DirectExchange exchangeDLQ) {
        return BindingBuilder.bind(colaIADLQ)
                .to(exchangeDLQ)
                .with(NombresCola.IA_PROCESAMIENTO_DLQ);
    }

    /**
     * Define el exchange de pagos.
     */
    @Bean
    public TopicExchange exchangePagos() {
        return new TopicExchange(NombresExchange.PAGOS, true, false);
    }

    /**
     * Realiza el enlace entre la cola de pagos de financiero y el exchange de pagos.
     */
    @Bean
    public Binding bindingPagosFinanciero(
            @Qualifier("colaPagosExitosos") Queue colaPagosExitosos,
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
                .bind(colaPagosExitosos)
                .to(exchangePagos)
                .with(RoutingKeys.PAGO_EXITOSO);
    }
}

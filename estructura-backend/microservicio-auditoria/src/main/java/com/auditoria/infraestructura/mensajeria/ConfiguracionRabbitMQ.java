package com.auditoria.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.ConfiguracionRabbitBase;
import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la topología de RabbitMQ para el Microservicio de Auditoría.
 * <p>
 * Esta clase extiende de {@link ConfiguracionRabbitBase} para heredar la
 * infraestructura base (conexión, serialización JSON) y define la estructura
 * de Exchanges, Colas y Bindings utilizando las constantes de la librería común.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.4
 * @since 2026-05
 */
@Configuration
public class ConfiguracionRabbitMQ extends ConfiguracionRabbitBase {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    /**
     * Define el Exchange principal de tipo Topic para el enrutamiento de auditoría.
     * 
     * @return {@link TopicExchange} configurado como durable.
     */
    @Bean
    public TopicExchange exchangeAuditoria() {
        return ExchangeBuilder
                .topicExchange(NombresExchange.AUDITORIA)
                .durable(true)
                .build();
    }

    /**
     * Define el Dead Letter Exchange (DLX) para gestionar mensajes fallidos.
     * 
     * @return {@link DirectExchange} configurado como durable.
     */
    @Bean
    public DirectExchange exchangeAuditoriaDlq() {
        return ExchangeBuilder
                .directExchange(NombresExchange.AUDITORIA_DLX)
                .durable(true)
                .build();
    }

    // =========================================================================
    // COLAS PRINCIPALES (Con soporte para DLQ)
    // =========================================================================

    /**
     * Configura la cola para eventos de acceso con redirección a DLQ y TTL.
     * 
     * @return {@link Queue} con argumentos de Dead Lettering y tiempo de vida de 10
     *         min.
     */
    @Bean
    public Queue colaAccesos() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_ACCESOS)
                .withArgument("x-dead-letter-exchange", NombresExchange.AUDITORIA_DLX)
                .withArgument("x-dead-letter-routing-key", RoutingKeys.DLQ_AUDITORIA_ACCESO)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    /**
     * Configura la cola para otros eventos con redirección a DLQ y TTL.
     * 
     * @return {@link Queue} con argumentos de Dead Lettering y tiempo de vida de 10
     *         min.
     */
    @Bean
    public Queue colaAuditoria() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_EVENTOS)
                .withArgument("x-dead-letter-exchange", NombresExchange.AUDITORIA_DLX)
                .withArgument("x-dead-letter-routing-key", RoutingKeys.DLQ_AUDITORIA_EVENTO)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    /**
     * Configura la cola para trazas transaccionales.
     * 
     * @return {@link Queue} configurada para resiliencia en eventos críticos
     *         financieros.
     */
    @Bean
    public Queue colaTransacciones() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_TRANSACCIONES)
                .withArgument("x-dead-letter-exchange", NombresExchange.AUDITORIA_DLX)
                .withArgument("x-dead-letter-routing-key", RoutingKeys.DLQ_AUDITORIA_TRANSACCIONAL)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    // =========================================================================
    // DEAD LETTER QUEUES (DLQ)
    // =========================================================================

    /**
     * Define la cola física donde se almacenarán los mensajes de acceso que fallen.
     * 
     * @return {@link Queue} durable para persistencia de fallos.
     */
    @Bean
    public Queue colaAccesosDlq() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_ACCESOS_DLQ)
                .build();
    }

    /**
     * Define la cola física donde se almacenarán los mensajes de eventos que fallen.
     * 
     * @return {@link Queue} durable para persistencia de fallos.
     */
    @Bean
    public Queue colaEventosDlq() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_EVENTOS_DLQ)
                .build();
    }

    /**
     * Define la cola física donde se almacenarán los mensajes de acceso que fallen.
     * 
     * @return {@link Queue} durable para persistencia de fallos.
     */
    @Bean
    public Queue colaTransaccionesDlq() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_TRANSACCIONES_DLQ)
                .build();
    }

    // =========================================================================
    // BINDINGS (Vinculaciones)
    // =========================================================================

    /**
     * Vincula la cola de accesos al exchange principal mediante su Routing Key.
     * 
     * @param colaAccesos       Bean de la cola de accesos.
     * @param exchangeAuditoria Bean del exchange de auditoría.
     * @return {@link Binding} que establece la ruta de mensajes de acceso.
     */
    @Bean
    public Binding bindingAccesos(Queue colaAccesos, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAccesos)
                .to(exchangeAuditoria)
                .with(RoutingKeys.AUDITORIA_ACCESO_ALL);
    }

    /**
     * Vincula la cola de eventos al exchange principal mediante su Routing Key.
     * 
     * @param colaEventos       Bean de la cola de eventos.
     * @param exchangeAuditoria Bean del exchange de auditoría.
     * @return {@link Binding} que establece la ruta de mensajes de acceso.
     */
    @Bean
    public Binding bindingEventos(Queue colaEventos, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaEventos)
                .to(exchangeAuditoria)
                .with(RoutingKeys.AUDITORIA_EVENTO_ALL);
    }

    /**
     * Vincula la cola transaccional al exchange principal.
     * 
     * @param colaTransacciones Bean de la cola transaccional.
     * @param exchangeAuditoria Bean del exchange de auditoría.
     * @return {@link Binding} para eventos de cambio de entidades.
     */
    @Bean
    public Binding bindingTransacciones(Queue colaTransacciones, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaTransacciones)
                .to(exchangeAuditoria)
                .with(RoutingKeys.AUDITORIA_TRANSACCION_ALL);
    }

    /**
     * Vincula la DLQ de accesos al exchange de fallos.
     * 
     * @param colaAccesosDlq       Bean de la cola DLQ.
     * @param exchangeAuditoriaDlq Bean del exchange DLX.
     * @return {@link Binding} para el enrutamiento de mensajes rechazados.
     */
    @Bean
    public Binding bindingAccesosDlq(Queue colaAccesosDlq, DirectExchange exchangeAuditoriaDlq) {
        return BindingBuilder
                .bind(colaAccesosDlq)
                .to(exchangeAuditoriaDlq)
                .with(RoutingKeys.DLQ_AUDITORIA_ACCESO);
    }

    /**
     * Vincula la DLQ de eventos al exchange de fallos.
     * 
     * @param colaEventosDlq       Bean de la cola DLQ.
     * @param exchangeAuditoriaDlq Bean del exchange DLX.
     * @return {@link Binding} para el enrutamiento de mensajes rechazados.
     */
    @Bean
    public Binding bindingEventosDlq(Queue colaEventosDlq, DirectExchange exchangeAuditoriaDlq) {
        return BindingBuilder
                .bind(colaEventosDlq)
                .to(exchangeAuditoriaDlq)
                .with(RoutingKeys.DLQ_AUDITORIA_EVENTO);
    }

    /**
     * Vincula la DLQ de accesos al exchange de fallos.
     * 
     * @param colaAccesosDlq       Bean de la cola DLQ.
     * @param exchangeAuditoriaDlq Bean del exchange DLX.
     * @return {@link Binding} para el enrutamiento de mensajes rechazados.
     */
    @Bean
    public Binding bindingTransaccionesDlq(Queue colaTransaccionesDlq, DirectExchange exchangeAuditoriaDlq) {
        return BindingBuilder
                .bind(colaTransaccionesDlq)
                .to(exchangeAuditoriaDlq)
                .with(RoutingKeys.DLQ_AUDITORIA_TRANSACCIONAL);
    }
}
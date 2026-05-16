package com.cliente.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la topología de RabbitMQ para el Microservicio de Cliente.
 * <p>
 * Define la estructura de Exchanges, Colas y Bindings utilizando las constantes
 * de la librería común.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.4
 * @since 2026-09
 */
@Configuration
public class ConfiguracionRabbitMQ {

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

    // =========================================================================
    // DEAD LETTER QUEUES (DLQ)
    // =========================================================================

    /**
     * Define la cola física donde se almacenarán los mensajes de eventos que
     * fallen.
     * 
     * @return {@link Queue} durable para persistencia de fallos.
     */
    @Bean
    public Queue colaEventosDlq() {
        return QueueBuilder
                .durable(NombresCola.AUDITORIA_EVENTOS_DLQ)
                .build();
    }

    // =========================================================================
    // BINDINGS (Vinculaciones)
    // =========================================================================

    /**
     * Vincula la cola de eventos al exchange principal mediante su Routing Key.
     * 
     * @param colaEventos       Bean de la cola de eventos.
     * @param exchangeAuditoria Bean del exchange de auditoría.
     * @return {@link Binding} que establece la ruta de mensajes de acceso.
     */
    public Binding bindingEventos(
            @Qualifier("colaAuditoria") Queue colaEventos,
            @Qualifier("exchangeAuditoria") TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaEventos)
                .to(exchangeAuditoria)
                .with(RoutingKeys.AUDITORIA_EVENTO_ALL);
    }

    // =========================================================================
    // SINCRONIZACIÓN CLIENTE → IA (Tiempo Real)
    // =========================================================================

    /**
     * Exchange de tipo Topic para la sincronización de cambios del cliente.
     *
     * @return {@link TopicExchange} durable para sincronización.
     */
    @Bean
    public TopicExchange exchangeClienteActualizaciones() {
        return ExchangeBuilder
                .topicExchange(NombresExchange.CLIENTE_ACTUALIZACIONES)
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange (DLX) para mensajes de sincronización fallidos.
     *
     * @return {@link DirectExchange} durable para mensajes de error.
     */
    @Bean
    public DirectExchange exchangeClienteActualizacionesDlx() {
        return ExchangeBuilder
                .directExchange(NombresExchange.CLIENTE_ACTUALIZACIONES_DLX)
                .durable(true)
                .build();
    }

    /**
     * Cola donde el ms-ia recibe las actualizaciones del contexto financiero.
     * <p>
     * Configurada con Dead Letter Exchange y TTL. Si un mensaje falla tras
     * 3 reintentos (controlados por el consumidor Python), se redirige a
     * {@code cola.ia.sincronizacion.error} para análisis posterior.
     * </p>
     *
     * @return {@link Queue} durable con soporte para DLQ.
     */
    @Bean
    public Queue colaSincronizacionContexto() {
        return QueueBuilder
                .durable(NombresCola.IA_SINCRONIZACION_CONTEXTO)
                .withArgument("x-dead-letter-exchange", NombresExchange.CLIENTE_ACTUALIZACIONES_DLX)
                .withArgument("x-dead-letter-routing-key", NombresCola.IA_SINCRONIZACION_ERROR)
                .withArgument("x-message-ttl", 600000) // 10 minutos
                .build();
    }

    /**
     * Cola de error donde se almacenan los mensajes de sincronización
     * que no pudieron ser procesados exitosamente.
     *
     * @return {@link Queue} durable para persistencia de fallos.
     */
    @Bean
    public Queue colaSincronizacionError() {
        return QueueBuilder
                .durable(NombresCola.IA_SINCRONIZACION_ERROR)
                .build();
    }

    /**
     * Vincula la cola de sincronización al exchange de actualizaciones de cliente.
     *
     * @param colaSincronizacionContexto     Bean de la cola de sincronización.
     * @param exchangeClienteActualizaciones Bean del exchange de actualizaciones.
     * @return {@link Binding} con routing key {@code cliente.perfil.actualizado}.
     */
    @Bean
    public Binding bindingSincronizacionIA(
            @Qualifier("colaSincronizacionContexto") Queue colaSincronizacionContexto,
            @Qualifier("exchangeClienteActualizaciones") TopicExchange exchangeClienteActualizaciones) {
        return BindingBuilder
                .bind(colaSincronizacionContexto)
                .to(exchangeClienteActualizaciones)
                .with(RoutingKeys.CLIENTE_PERFIL_ACTUALIZADO);
    }

    /**
     * Vincula la cola de error al DLX de sincronización.
     *
     * @param colaSincronizacionError           Bean de la cola de error.
     * @param exchangeClienteActualizacionesDlx Bean del DLX.
     * @return {@link Binding} directo a la cola de error.
     */
    @Bean
    public Binding bindingSincronizacionDlq(
            @Qualifier("colaSincronizacionError") Queue colaSincronizacionError,
            @Qualifier("exchangeClienteActualizacionesDlx") DirectExchange exchangeClienteActualizacionesDlx) {
        return BindingBuilder
                .bind(colaSincronizacionError)
                .to(exchangeClienteActualizacionesDlx)
                .with(NombresCola.IA_SINCRONIZACION_ERROR);
    }
}

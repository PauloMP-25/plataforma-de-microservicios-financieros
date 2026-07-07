package com.suscripciones.infraestructura.configuracion;

import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la topología de RabbitMQ para el microservicio de suscripciones.
 * Declara el exchange de pagos y la cola dedicada para recibir notificaciones de pagos de planes.
 */
@Configuration
public class ConfiguracionRabbitMQ {

    public static final String COLA_SUSCRIPCIONES_PAGOS = "cola.suscripciones.pagos.exitosos";

    @Bean
    public TopicExchange exchangePagos() {
        return new TopicExchange(NombresExchange.PAGOS, true, false);
    }

    @Bean
    public Queue queueSuscripcionPagosExitosos() {
        return QueueBuilder.durable(COLA_SUSCRIPCIONES_PAGOS).build();
    }

    @Bean
    public Binding bindingSuscripcionPagosExitosos(
            @Qualifier("queueSuscripcionPagosExitosos") Queue queueSuscripcionPagosExitosos,
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
                .bind(queueSuscripcionPagosExitosos)
                .to(exchangePagos)
                .with(RoutingKeys.PAGO_EXITOSO);
    }

    // --- NUEVA CONFIGURACIÓN DE LOGIN Y EVENTOS ---
    
    public static final String COLA_SUSCRIPCIONES_LOGIN_EXITOSO = "cola.suscripciones.login.exitoso";
    public static final String EXCHANGE_SUSCRIPCIONES_EVENTOS = "exchange.suscripciones.eventos";
    
    @Bean
    public Queue queueSuscripcionLoginExitoso() {
        return QueueBuilder.durable(COLA_SUSCRIPCIONES_LOGIN_EXITOSO).build();
    }
    
    @Bean
    public TopicExchange exchangeUsuarioEventosSuscripciones() {
        return new TopicExchange(NombresExchange.USUARIO_EVENTOS, true, false);
    }

    @Bean
    public Binding bindingSuscripcionLoginExitoso(
            @Qualifier("queueSuscripcionLoginExitoso") Queue queueSuscripcionLoginExitoso,
            @Qualifier("exchangeUsuarioEventosSuscripciones") TopicExchange exchangeUsuarioEventosSuscripciones) {
        return BindingBuilder
                .bind(queueSuscripcionLoginExitoso)
                .to(exchangeUsuarioEventosSuscripciones)
                .with(RoutingKeys.USUARIO_LOGIN_EXITOSO);
    }

    @Bean
    public TopicExchange exchangeSuscripcionesEventos() {
        return new TopicExchange(EXCHANGE_SUSCRIPCIONES_EVENTOS, true, false);
    }
}

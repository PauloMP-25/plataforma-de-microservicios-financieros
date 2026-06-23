package com.pagos.infraestructura.configuracion;

import com.libreria.comun.mensajeria.NombresCola;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura para RabbitMQ.
 * Define el Exchange de tipo Topic y las colas vinculadas mediante Routing Keys.
 */
@Configuration
public class ConfiguracionColasPagos {

    @Bean
    public TopicExchange exchangePagos() {
        return new TopicExchange(NombresExchange.PAGOS, true, false);
    }

    @Bean
    public Queue colaUsuarioPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_USUARIO).build();
    }

    @Bean
    public Binding bindingUsuarioPagoExitoso(
            @Qualifier("colaUsuarioPagosExitosos") Queue colaUsuarioPagosExitosos, 
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaUsuarioPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }

    @Bean
    public Queue colaMensajeriaPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_MENSAJERIA).build();
    }

    @Bean
    public Binding bindingMensajeriaPagoExitoso(
            @Qualifier("colaMensajeriaPagosExitosos") Queue colaMensajeriaPagosExitosos, 
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaMensajeriaPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }

    @Bean
    public Queue colaFinancieroPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_FINANCIERO).build();
    }

    @Bean
    public Binding bindingFinancieroPagoExitoso(
            @Qualifier("colaFinancieroPagosExitosos") Queue colaFinancieroPagosExitosos, 
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaFinancieroPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }

    @Bean
    public Queue colaAuditoriaPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS_AUDITORIA).build();
    }

    @Bean
    public Binding bindingAuditoriaPagoExitoso(
            @Qualifier("colaAuditoriaPagosExitosos") Queue colaAuditoriaPagosExitosos, 
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaAuditoriaPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }

    // ── Suscripciones — duplicado explícito para visibilidad completa de topología ──────────────

    /**
     * Cola de ms-suscripciones. Se declara también aquí para que la topología de
     * fan-out sea completamente visible desde el lado publicador (ms-pago).
     * RabbitMQ es idempotente en la declaración de colas: si ya existe con los
     * mismos parámetros, no genera conflicto.
     */
    @Bean
    public Queue colaSuscripcionesPagosExitosos() {
        return QueueBuilder.durable("cola.suscripciones.pagos.exitosos").build();
    }

    @Bean
    public Binding bindingSuscripcionesPagoExitoso(
            @Qualifier("colaSuscripcionesPagosExitosos") Queue colaSuscripcionesPagosExitosos,
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaSuscripcionesPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }
}

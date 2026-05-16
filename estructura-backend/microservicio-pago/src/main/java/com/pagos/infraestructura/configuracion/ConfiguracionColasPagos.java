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
    public Queue colaPagosExitosos() {
        return QueueBuilder.durable(NombresCola.PAGOS_EXITOSOS).build();
    }

    @Bean
    public Binding bindingPagoExitoso(
            @Qualifier("colaPagosExitosos") Queue colaPagosExitosos, 
            @Qualifier("exchangePagos") TopicExchange exchangePagos) {
        return BindingBuilder
            .bind(colaPagosExitosos)
            .to(exchangePagos)
            .with(RoutingKeys.PAGO_EXITOSO);
    }
}

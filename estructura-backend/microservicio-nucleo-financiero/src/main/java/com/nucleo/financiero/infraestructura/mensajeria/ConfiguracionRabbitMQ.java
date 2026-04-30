package com.nucleo.financiero.infraestructura.mensajeria;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    // Nombres de los elementos
    public static final String EXCHANGE_IA = "exchange.ia";
    public static final String COLA_IA = "cola.ia.procesamiento";
    public static final String RK_IA = "transaccion.nueva";

    /**
     * Define el Exchange de tipo Topic para la comunicación con la IA.
     */
    @Bean
    public TopicExchange exchangeIA() {
        return new TopicExchange(EXCHANGE_IA);
    }

    /**
     * Define la cola donde el microservicio de Python escuchará.
     */
    @Bean
    public Queue colaIA() {
        return QueueBuilder.durable(COLA_IA).build();
    }

    /**
     * Une la cola con el exchange usando la routing key.
     */
    @Bean
    public Binding bindingIA(Queue colaIA, TopicExchange exchangeIA) {
        return BindingBuilder.bind(colaIA).to(exchangeIA).with(RK_IA);
    }

    /**
     * Configura el convertidor para enviar objetos como JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

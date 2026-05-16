package com.libreria.comun.autoconfiguracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import com.libreria.comun.seguridad.ServicioJwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Registro automático de los componentes de la librería LUKA COMMONS.
 * <p>
 * Gracias a esta configuración, los microservicios solo necesitan incluir
 * la dependencia para tener acceso a la seguridad, mensajería y manejo de
 * errores.
 * </p>
 */
@AutoConfiguration
public class LukaConfiguracion {

    @Value("${spring.application.name:luka-service}")
    private String applicationName;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    // --- SEGURIDAD ---

    @Bean
    @ConditionalOnMissingBean
    public ServicioJwt servicioJwt() {
        return new ServicioJwt();
    }

    @Bean
    @ConditionalOnMissingBean
    public FiltroJwt filtroJwt(ServicioJwt servicioJwt) {
        return new FiltroJwt(servicioJwt);
    }

    @Bean
    @ConditionalOnMissingBean
    public PuntoEntradaJwt puntoEntradaJwt(ObjectMapper objectMapper) {
        return new PuntoEntradaJwt(objectMapper);
    }

    // --- EXCEPCIONES ---

    @Bean
    @ConditionalOnMissingBean
    public ManejadorGlobalExcepcionesBase manejadorGlobalExcepciones() {
        return new ManejadorGlobalExcepcionesBase() {
        };
    }

    // --- MENSAJERÍA (RABBITMQ) ---

    @SuppressWarnings("null")
    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUsername);
        factory.setPassword(rabbitPassword);
        factory.setConnectionNameStrategy(cf -> applicationName);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @SuppressWarnings("null")
    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicadorEventosBase publicadorEventosBase(RabbitTemplate rabbitTemplate) {
        return new PublicadorEventosBase(rabbitTemplate);
    }
}

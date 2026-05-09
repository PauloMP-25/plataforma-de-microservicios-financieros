package com.libreria.comun.mensajeria;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de infraestructura para RabbitMQ.
 * <p>
 * Define la serialización JSON (Jackson) obligatoria para la interoperabilidad
 * con el microservicio de IA en Python y asegura una gestión de conexiones
 * eficiente mediante nombres de aplicación dinámicos.
 * </p>
 * 
 * @author Paulo Moron
 */
@Configuration
public class ConfiguracionRabbitBase {

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

    /**
     * Configura la conexión con el broker RabbitMQ asignando el nombre del 
     * microservicio a la conexión para facilitar el monitoreo en el Management Plugin.
     * @return 
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUsername);
        factory.setPassword(rabbitPassword);
        factory.setConnectionNameStrategy(cf -> applicationName);
        return factory;
    }

    /**
     * Convertidor de mensajes a JSON. 
     * Esencial para que Python (IA) pueda leer los mensajes enviados desde Java.
     * @return 
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Template de Rabbit pre-configurado con el convertidor JSON.
     * @param connectionFactory
     * @return 
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Fábrica de contenedores para los @RabbitListener.
     * Configura un prefetchCount de 1 para garantizar un balanceo de carga justo entre hilos.
     * @param connectionFactory
     * @return 
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
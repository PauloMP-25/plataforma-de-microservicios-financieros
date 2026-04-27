package com.mensajeria.infraestructura.mensajeria;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    @Value("${spring.application.name:microservicio-mensajeria}")
    private String applicationName;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    // Exchanges
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";
    public static final String EXCHANGE_MENSAJERIA = "exchange.mensajeria";

    // Colas
    public static final String COLA_OTP_GENERAR = "cola.mensajeria.otp.generar";

    // Routing Keys
    public static final String RK_OTP_GENERAR = "mensaje.otp.generar";

    @Bean
    public ConnectionFactory connectionFactory() {
        // 1. Configuramos la fábrica con tus credenciales reales
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUsername);
        factory.setPassword(rabbitPassword);

        // 2. Mantenemos tu estrategia de nombre personalizado
        factory.setConnectionNameStrategy(connectionFactory -> applicationName);

        return factory;
    }

    @Bean
    public TopicExchange exchangeMensajeria() {
        return new TopicExchange(EXCHANGE_MENSAJERIA);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SERIALIZACIÓN JSON
    // ══════════════════════════════════════════════════════════════════════════
    @Bean
    public Queue colaOtpGenerar() {
        return new Queue(COLA_OTP_GENERAR, true);
    }

    @Bean
    public Binding bindingOtpGenerar(Queue colaOtpGenerar, TopicExchange exchangeMensajeria) {
        return BindingBuilder.bind(colaOtpGenerar).to(exchangeMensajeria).with(RK_OTP_GENERAR);
    }

    /**
     * Convierte automáticamente objetos Java ↔ JSON en los mensajes AMQP. Sin
     * este bean, Spring AMQP usaría serialización binaria de Java (frágil). Con
     * él, los mensajes son legibles desde cualquier lenguaje.
     *
     * @return
     */
    @Bean
    public org.springframework.amqp.support.converter.MessageConverter convertidorMensajesJson() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura el RabbitTemplate con el convertidor JSON. Es el cliente usado
     * por los productores para publicar mensajes.
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(convertidorMensajesJson());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(convertidorMensajesJson());
        return factory;
    }
}

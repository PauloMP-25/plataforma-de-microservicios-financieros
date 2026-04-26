package com.mensajeria.infraestructura.mensajeria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionRabbitMQ {

    // 1. Definimos el nombre del Exchange (debe ser igual al de Auditoría)
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";

    // ══════════════════════════════════════════════════════════════════════════
    // SERIALIZACIÓN JSON
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Convierte automáticamente objetos Java ↔ JSON en los mensajes AMQP.
     * Sin este bean, Spring AMQP usaría serialización binaria de Java (frágil).
     * Con él, los mensajes son legibles desde cualquier lenguaje.
     * @return 
     */
    @Bean
    public org.springframework.amqp.support.converter.MessageConverter convertidorMensajesJson() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura el RabbitTemplate con el convertidor JSON.
     * Es el cliente usado por los productores para publicar mensajes.
     * @param connectionFactory
     * @return 
     */
    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(convertidorMensajesJson());
        return template;
    }
}


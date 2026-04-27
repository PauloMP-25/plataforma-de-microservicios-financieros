package com.auditoria.infraestructura.mensajeria;

import org.springframework.amqp.core.*;
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
 * Configuración de infraestructura RabbitMQ para Auditoría.
 * * Implementa un modelo de mensajería basado en Topic Exchange para el enrutamiento
 * de eventos de acceso y transacciones. Incluye soporte para resiliencia mediante:
 * - Dead Letter Exchange (DLX) para manejo de mensajes fallidos.
 * - TTL (Time-To-Live) de 10 minutos para mensajes en cola.
 * - Routing Keys con patrones wildcards (auditoria.#).
 */
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
    // ══════════════════════════════════════════════════════════════════════════
    // CONSTANTES — Nombres de Exchanges, Colas y Routing Keys
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Exchange principal: enruta mensajes a las colas de auditoría.
     */
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";

    /**
     * Dead Letter Exchange: recibe mensajes que fallaron tras los reintentos.
     */
    public static final String EXCHANGE_AUDITORIA_DLQ = "exchange.auditoria.dlq";

    // ── Colas principales ─────────────────────────────────────────────────────
    /**
     * Recibe eventos de acceso (login, logout, intentos fallidos).
     */
    public static final String COLA_ACCESOS = "cola.auditoria.accesos";

    /**
     * Recibe eventos de cambios en entidades de negocio (trazabilidad).
     */
    public static final String COLA_TRANSACCIONES = "cola.auditoria.transacciones";

    // ── Dead Letter Queues (DLQ) ──────────────────────────────────────────────
    /**
     * Mensajes de acceso que no pudieron procesarse después de los reintentos.
     */
    public static final String COLA_ACCESOS_DLQ = "cola.auditoria.accesos.dlq";

    /**
     * Mensajes transaccionales que no pudieron procesarse.
     */
    public static final String COLA_TRANSACCIONES_DLQ = "cola.auditoria.transacciones.dlq";

    // ── Routing Keys ──────────────────────────────────────────────────────────
    /**
     * Patrón para eventos de acceso. El '#' acepta cualquier sufijo.
     */
    public static final String RK_ACCESO = "auditoria.acceso.#";

    /**
     * Patrón para eventos transaccionales.
     */
    public static final String RK_TRANSACCION = "auditoria.transaccion.#";

    /**
     * Routing key usada por las colas para reenviar mensajes muertos a la DLQ.
     */
    public static final String RK_ACCESOS_DLQ = "dlq.auditoria.accesos";
    public static final String RK_TRANSACCIONES_DLQ = "dlq.auditoria.transacciones";

    public static final String RK_EVENTOS_SEGURIDAD = "auditoria.evento.#";

    /**
     * Define la fábrica de conexiones y asigna un nombre identificable 
     * en el panel de administración de RabbitMQ.
     */
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
    
    // ══════════════════════════════════════════════════════════════════════════
    // EXCHANGES
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Exchange principal de tipo Topic. Permite enrutar mensajes por patrones
     * de routing key (ej: auditoria.acceso.*). durable=true: sobrevive
     * reinicios del broker.
     *
     * @return
     */
    @Bean
    public TopicExchange exchangeAuditoria() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_AUDITORIA)
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Exchange: recibe mensajes rechazados o que expiraron. Las
     * colas principales apuntan aquí via x-dead-letter-exchange.
     *
     * @return
     */
    @Bean
    public DirectExchange exchangeAuditoriaDlq() {
        return ExchangeBuilder
                .directExchange(EXCHANGE_AUDITORIA_DLQ)
                .durable(true)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COLAS PRINCIPALES (con configuración DLQ integrada)
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Cola principal para eventos de acceso.
     *
     * Argumentos clave: - x-dead-letter-exchange: hacia dónde enviar mensajes
     * que fallan - x-dead-letter-routing-key: routing key en el DLX -
     * x-message-ttl: tiempo máximo de vida en ms antes de morir (10 min)
     *
     * @return
     */
    @Bean
    public Queue colaAccesos() {
        return QueueBuilder
                .durable(COLA_ACCESOS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_AUDITORIA_DLQ)
                .withArgument("x-dead-letter-routing-key", RK_ACCESOS_DLQ)
                .withArgument("x-message-ttl", 600_000) // 10 minutos
                .build();
    }

    /**
     * Cola principal para eventos de trazabilidad transaccional. Misma
     * configuración de DLQ que la cola de accesos.
     *
     * @return
     */
    @Bean
    public Queue colaTransacciones() {
        return QueueBuilder
                .durable(COLA_TRANSACCIONES)
                .withArgument("x-dead-letter-exchange", EXCHANGE_AUDITORIA_DLQ)
                .withArgument("x-dead-letter-routing-key", RK_TRANSACCIONES_DLQ)
                .withArgument("x-message-ttl", 600_000) // 10 minutos
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DEAD LETTER QUEUES (DLQ)
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * DLQ para mensajes de acceso fallidos. Sin TTL ni DLX propio: los mensajes
     * aquí esperan revisión manual o un proceso de reprocesamiento programado.
     *
     * @return
     */
    @Bean
    public Queue colaAccesosDlq() {
        return QueueBuilder
                .durable(COLA_ACCESOS_DLQ)
                .build();
    }

    /**
     * DLQ para mensajes transaccionales fallidos. En un entorno FinTech, estos
     * mensajes NO se deben perder jamás.
     *
     * @return
     */
    @Bean
    public Queue colaTransaccionesDlq() {
        return QueueBuilder
                .durable(COLA_TRANSACCIONES_DLQ)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BINDINGS — Conectan exchanges con colas usando routing keys
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Enlaza la cola de accesos al exchange principal. El patrón
     * "auditoria.acceso.#" acepta cualquier routing key que comience con
     * "auditoria.acceso." (ej: auditoria.acceso.login,
     * auditoria.acceso.logout).
     *
     * @param colaAccesos
     * @param exchangeAuditoria
     * @return
     */
    @Bean
    public Binding bindingAccesos(Queue colaAccesos, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAccesos)
                .to(exchangeAuditoria)
                .with(RK_ACCESO);
    }

    /**
     * Enlaza la cola de transacciones al exchange principal. Acepta cualquier
     * routing key con prefijo "auditoria.transaccion."
     *
     * @param colaTransacciones
     * @param exchangeAuditoria
     * @return
     */
    @Bean
    public Binding bindingTransacciones(Queue colaTransacciones, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaTransacciones)
                .to(exchangeAuditoria)
                .with(RK_TRANSACCION);
    }

    /**
     * Binding de la DLQ de accesos al Dead Letter Exchange. Usa routing key
     * exacta (Direct Exchange no soporta wildcards).
     *
     * @param colaAccesosDlq
     * @param exchangeAuditoriaDlq
     * @return
     */
    @Bean
    public Binding bindingAccesosDlq(Queue colaAccesosDlq, DirectExchange exchangeAuditoriaDlq) {
        return BindingBuilder
                .bind(colaAccesosDlq)
                .to(exchangeAuditoriaDlq)
                .with(RK_ACCESOS_DLQ);
    }

    /**
     * Binding de la DLQ de transacciones al Dead Letter Exchange.
     *
     * @param colaTransaccionesDlq
     * @param exchangeAuditoriaDlq
     * @return
     */
    @Bean
    public Binding bindingTransaccionesDlq(Queue colaTransaccionesDlq, DirectExchange exchangeAuditoriaDlq) {
        return BindingBuilder
                .bind(colaTransaccionesDlq)
                .to(exchangeAuditoriaDlq)
                .with(RK_TRANSACCIONES_DLQ);
    }

    @Bean
    public Binding bindingEventosSeguridad(Queue colaAccesos, TopicExchange exchangeAuditoria) {
        return BindingBuilder
                .bind(colaAccesos)
                .to(exchangeAuditoria)
                .with(RK_EVENTOS_SEGURIDAD);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SERIALIZACIÓN JSON
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Convierte automáticamente objetos Java ↔ JSON en los mensajes AMQP. Sin
     * este bean, Spring AMQP usaría serialización binaria de Java (frágil). Con
     * él, los mensajes son legibles desde cualquier lenguaje.
     *
     * @return
     */
    @Bean
    public MessageConverter convertidorMensajesJson() {
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(convertidorMensajesJson());
        return template;
    }

    /**
     * Configura el contenedor de listeners con el convertidor JSON. Asegura que
     * los @RabbitListener también deserialicen desde JSON.
     *
     * prefetchCount=1: el consumidor procesa un mensaje a la vez antes de pedir
     * otro. Evita que un consumidor lento acumule mensajes en memoria.
     *
     * @param connectionFactory
     * @param messageConverter
     * @return
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(1);
        // AcknowledgeMode.AUTO: Spring confirma automáticamente si no hay excepción,
        // rechaza si se lanza una excepción (y el mensaje va a la DLQ).
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}

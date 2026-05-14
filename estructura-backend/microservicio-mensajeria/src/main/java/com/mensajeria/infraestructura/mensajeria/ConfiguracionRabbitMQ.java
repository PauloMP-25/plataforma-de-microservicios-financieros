package com.mensajeria.infraestructura.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.libreria.comun.mensajeria.ConfiguracionRabbitBase;

/**
 * Configuración de RabbitMQ para el microservicio de mensajería.
 * <p>
 * Hereda la infraestructura base (ConnectionFactory, RabbitTemplate,
 * convertidor
 * JSON) de {@link ConfiguracionRabbitBase} y define la topología específica de
 * este servicio:
 * <ul>
 * <li>Exchange principal: {@code exchange.mensajeria} (TopicExchange).</li>
 * <li>Cola OTP: {@code cola.mensajeria.otp.generar}, durable, con DLQ
 * configurada.</li>
 * <li>Dead Letter Queue: {@code cola.mensajeria.error} — recibe mensajes que
 * fallan 3 veces consecutivas.</li>
 * </ul>
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Configuration
public class ConfiguracionRabbitMQ extends ConfiguracionRabbitBase {

    // ── Exchanges ─────────────────────────────────────────────────────────────
    /** Exchange de auditoría compartido con otros microservicios del ecosistema. */
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";

    /** Exchange propio del microservicio de mensajería. */
    public static final String EXCHANGE_MENSAJERIA = "exchange.mensajeria";

    /** Exchange de error (Dead Letter) al que se envían mensajes no procesables. */
    public static final String EXCHANGE_ERROR = "exchange.mensajeria.error";

    // ── Colas ─────────────────────────────────────────────────────────────────
    /** Cola principal de generación de OTPs. */
    public static final String COLA_OTP_GENERAR = "cola.mensajeria.otp.generar";

    /** Cola de errores (DLQ) donde llegan mensajes con 3 fallos consecutivos. */
    public static final String COLA_ERROR = "cola.mensajeria.error";

    /** Cola para envío asíncrono de emails transaccionales. */
    public static final String COLA_EMAIL_ENVIAR = "cola.mensajeria.email.enviar";

    /** Cola para envío asíncrono de SMS. */
    public static final String COLA_SMS_ENVIAR = "cola.mensajeria.sms.enviar";

    // ── Routing Keys ──────────────────────────────────────────────────────────
    /** Routing key para solicitudes de generación de OTP. */
    public static final String RK_OTP_GENERAR = "mensaje.otp.generar";

    /** Routing key para errores y reenvíos a la DLQ. */
    public static final String RK_ERROR = "mensaje.error";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    /**
     * Exchange principal de mensajería del microservicio.
     *
     * @return {@link TopicExchange} durable con enrutamiento por patrón.
     */
    @Bean
    public TopicExchange exchangeMensajeria() {
        return new TopicExchange(EXCHANGE_MENSAJERIA);
    }

    /**
     * Exchange de error (Dead Letter Exchange) al que RabbitMQ re-enruta
     * mensajes rechazados después de agotar los reintentos.
     *
     * @return {@link TopicExchange} durable para mensajes fallidos.
     */
    @Bean
    public TopicExchange exchangeError() {
        return new TopicExchange(EXCHANGE_ERROR);
    }

    // ── Colas ─────────────────────────────────────────────────────────────────

    /**
     * Cola principal de OTP con política de Dead Letter configurada.
     * Los mensajes rechazados 3 veces son re-enrutados automáticamente a
     * {@code cola.mensajeria.error} para análisis posterior.
     *
     * @return {@link Queue} durable con {@code x-dead-letter-exchange} y
     *         {@code x-max-requeue-limit = 3}.
     */
    @Bean
    public Queue colaOtpGenerar() {
        return QueueBuilder.durable(COLA_OTP_GENERAR)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ERROR)
                .withArgument("x-dead-letter-routing-key", RK_ERROR)
                .withArgument("x-max-requeue-limit", 3)
                .build();
    }

    /**
     * Dead Letter Queue donde aterrizan los mensajes que fallaron 3 veces.
     * Permite inspección manual, alertas y eventual reintento controlado.
     *
     * @return {@link Queue} durable sin ninguna política adicional de DLQ.
     */
    @Bean
    public Queue colaError() {
        return QueueBuilder.durable(COLA_ERROR).build();
    }

    /**
     * Cola durable para el envío asíncrono de correos electrónicos transaccionales.
     *
     * @return {@link Queue} durable enlazada al exchange principal.
     */
    @Bean
    public Queue colaEmailEnviar() {
        return QueueBuilder.durable(COLA_EMAIL_ENVIAR)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ERROR)
                .withArgument("x-dead-letter-routing-key", RK_ERROR)
                .withArgument("x-max-requeue-limit", 3)
                .build();
    }

    /**
     * Cola durable para el envío asíncrono de SMS.
     *
     * @return {@link Queue} durable enlazada al exchange principal.
     */
    @Bean
    public Queue colaSmsEnviar() {
        return QueueBuilder.durable(COLA_SMS_ENVIAR)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ERROR)
                .withArgument("x-dead-letter-routing-key", RK_ERROR)
                .withArgument("x-max-requeue-limit", 3)
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    /**
     * Enlaza la cola OTP al exchange principal con la routing key de generación.
     *
     * @param colaOtpGenerar     Cola declarada por {@link #colaOtpGenerar()}.
     * @param exchangeMensajeria Exchange declarado por
     *                           {@link #exchangeMensajeria()}.
     * @return {@link Binding} que conecta cola y exchange.
     */
    @Bean
    public Binding bindingOtpGenerar(
            @Qualifier("colaOtpGenerar") Queue colaOtpGenerar,
            @Qualifier("exchangeMensajeria") TopicExchange exchangeMensajeria) {
        return BindingBuilder
                .bind(colaOtpGenerar)
                .to(exchangeMensajeria)
                .with(RK_OTP_GENERAR);
    }

    /**
     * Enlaza la cola de Email al exchange principal.
     * Los mensajes enviados con la routing key "mensaje.email.enviar" llegarán
     * aquí.
     */
    @Bean
    public Binding bindingEmailEnviar(
            @Qualifier("colaEmailEnviar") Queue colaEmailEnviar,
            @Qualifier("exchangeMensajeria") TopicExchange exchangeMensajeria) {
        return BindingBuilder
                .bind(colaEmailEnviar)
                .to(exchangeMensajeria)
                .with("mensaje.email.enviar"); // Puedes usar una constante si ya la tienes definida
    }

    /**
     * Enlaza la cola de SMS al exchange principal.
     * Los mensajes enviados con la routing key "mensaje.sms.enviar" llegarán aquí.
     */
    @Bean
    public Binding bindingSmsEnviar(
            @Qualifier("colaSmsEnviar") Queue colaSmsEnviar,
            @Qualifier("exchangeMensajeria") TopicExchange exchangeMensajeria) {
        return BindingBuilder
                .bind(colaSmsEnviar)
                .to(exchangeMensajeria)
                .with("mensaje.sms.enviar"); // Puedes usar una constante si ya la tienes definida
    }

    /**
     * Enlaza la DLQ al exchange de error con la routing key de error.
     *
     * @param colaError     Cola DLQ declarada por {@link #colaError()}.
     * @param exchangeError Exchange de error declarado por
     *                      {@link #exchangeError()}.
     * @return {@link Binding} que conecta la DLQ al exchange de error.
     */
    @Bean
    public Binding bindingError(
            @Qualifier("colaError") Queue colaError,
            @Qualifier("exchangeError") TopicExchange exchangeError) {
        return BindingBuilder
                .bind(colaError)
                .to(exchangeError)
                .with(RK_ERROR);
    }
}

package com.cliente.infraestructura.mensajeria;

import com.cliente.aplicacion.dtos.EventoAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publicador asíncrono de eventos de auditoría hacia RabbitMQ.
 *
 * <p>Publica en {@code exchange.auditoria} con routing key
 * {@code cola.auditoria}. El ms-auditoria consume de esa cola y persiste
 * el evento en la tabla {@code registros_auditoria}.</p>
 *
 * <p>Si RabbitMQ no está disponible el error se loguea pero NO se propaga
 * (patrón fire-and-forget). La operación principal ya se completó.</p>
 *
 * <p><b>IMPORTANTE:</b> {@code @Async} requiere que la llamada venga desde
 * un bean externo. Nunca llames a {@code publicar()} desde el mismo bean
 * que lo declara o Spring no creará el proxy y el método se ejecutará
 * de forma síncrona.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;

    /** Nombre del exchange declarado en ConfiguracionRabbitMQ */
    public static final String EXCHANGE_AUDITORIA  = "exchange.auditoria";
    /** Routing key — coincide con el nombre de la cola */
    public static final String ROUTING_KEY         = "cola.auditoria";
    /** Módulo identificador para los logs de auditoría */
    public static final String MODULO              = "MICROSERVICIO-CLIENTE";

    /**
     * Publica un evento de auditoría de forma asíncrona.
     *
     * @param evento objeto con todos los campos del evento
     */
    @Async
    public void publicar(EventoAuditoria evento) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_AUDITORIA, ROUTING_KEY, evento);
            log.debug("[AUDITORIA] Publicado: accion='{}' usuario='{}' modulo='{}'",
                    evento.accion(), evento.nombreUsuario(), evento.modulo());
        } catch (AmqpException ex) {
            log.error("[AUDITORIA] Fallo al publicar (no bloqueante): accion='{}' error='{}'",
                    evento.accion(), ex.getMessage());
        }
    }
}

package com.libreria.comun.mensajeria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.libreria.comun.enums.EstadoEvento;

/**
 * Publicador centralizado de eventos para LUKA APP.
 * <p>
 * Proporciona métodos específicos para los flujos más comunes (Auditoría, IA)
 * asegurando que se respete la topología de Topic Exchange.
 * </p>
 * 
 * @author Paulo Moron
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicadorEventosBase {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica eventos de acceso (Login, Logout, Fallos) de forma asíncrona.
     * Enruta a: exchange.auditoria -> auditoria.acceso.[accion]
     * 
     * @param dto    Contrato de datos de acceso.
     * @param estado EStado de la acción (ej: "login", "fallo", "logout").
     */
    @Async
    public void publicarAcceso(Object dto, EstadoEvento estado) {
        String rk = "auditoria.acceso." + estado.toString().toLowerCase();
        enviar(NombresExchange.AUDITORIA, rk, dto);
    }

    /**
     * Publica otros tipos de eventos (Recuperacion, Pagos, Descargas) de forma asíncrona.
     * Enruta a: exchange.auditoria -> auditoria.evento.[accion]
     * 
     * @param dto    Contrato de datos de acceso.
     * @param accion Etiqueta de la acción (ej: "login", "fallo", "logout").
     */
    @Async
    public void publicarEvento(Object dto, String accion) {
        String rk = "auditoria.evento" + accion.toLowerCase();
        enviar(NombresExchange.AUDITORIA, rk, dto);
    }

    /**
     * Publica eventos de trazabilidad transaccional.
     * Enruta a: exchange.auditoria -> auditoria.transaccion.[entidad]
     * 
     * @param dto     Contrato de datos de la transacción.
     * @param entidad Nombre de la entidad afectada (ej: "cliente", "cuenta").
     */
    @Async
    public void publicarTransaccion(Object dto, String entidad) {
        String rk = "auditoria.transaccion." + entidad.toLowerCase();
        enviar(NombresExchange.AUDITORIA, rk, dto);
    }

    /**
     * Envía solicitudes de análisis al microservicio de IA (Python).
     * 
     * @param dto Solicitud de análisis.
     */
    public void solicitarAnalisisIA(Object dto) {
        enviar(NombresExchange.IA, RoutingKeys.IA_ANALISIS_SOLICITAR, dto);
    }

    /**
     * Publica una actualización del contexto de cliente para sincronización en tiempo real.
     * <p>
     * Enruta a: exchange.cliente.actualizaciones → cliente.perfil.actualizado.
     * Los consumidores (ms-ia) reciben el {@code ContextoEstrategicoIADTO} completo
     * y actualizan su caché local sin necesidad de consultar la DB.
     * </p>
     *
     * @param dto       Contexto estratégico completo del cliente.
     * @param usuarioId ID del usuario para inyectar como header AMQP.
     */
    @Async
    public void publicarSincronizacionCliente(Object dto, String usuarioId) {
        enviarConHeaders(
                NombresExchange.CLIENTE_ACTUALIZACIONES,
                RoutingKeys.CLIENTE_PERFIL_ACTUALIZADO,
                dto,
                Map.of("usuarioId", usuarioId));
    }

    /**
     * Método base para el envío de mensajes al broker.
     *
     * @param exchange   Nombre del exchange.
     * @param routingKey Clave de enrutamiento.
     * @param payload    Objeto a serializar como JSON.
     */
    private void enviar(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.debug("[RABBIT-LIB] Mensaje enviado a {} con RK: {}", exchange, routingKey);
        } catch (AmqpException e) {
            log.error("[RABBIT-LIB-ERROR] Error al publicar: {}", e.getMessage());
        }
    }

    /**
     * Método de envío con headers AMQP personalizados.
     * <p>
     * Permite inyectar metadatos como el {@code usuarioId} en las propiedades
     * del mensaje para que el consumidor pueda identificar al destinatario
     * sin necesidad de deserializar el payload.
     * </p>
     *
     * @param exchange   Nombre del exchange.
     * @param routingKey Clave de enrutamiento.
     * @param payload    Objeto a serializar como JSON.
     * @param headers    Mapa de headers a inyectar en el mensaje AMQP.
     */
    private void enviarConHeaders(String exchange, String routingKey, Object payload,
            Map<String, Object> headers) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
                return message;
            });
            log.debug("[RABBIT-LIB] Mensaje con headers enviado a {} con RK: {}", exchange, routingKey);
        } catch (AmqpException e) {
            log.error("[RABBIT-LIB-ERROR] Error al publicar con headers: {}", e.getMessage());
        }
    }
}
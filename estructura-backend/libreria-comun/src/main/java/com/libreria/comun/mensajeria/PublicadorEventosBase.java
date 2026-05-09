package com.libreria.comun.mensajeria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
     * @param accion Etiqueta de la acción (ej: "login", "fallo", "logout").
     */
    @Async
    public void publicarAcceso(Object dto, String accion) {
        String rk = "auditoria.acceso." + accion.toLowerCase();
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
     * Método base para el envío de mensajes al broker.
     */
    private void enviar(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.debug("[RABBIT-LIB] Mensaje enviado a {} con RK: {}", exchange, routingKey);
        } catch (AmqpException e) {
            log.error("[RABBIT-LIB-ERROR] Error al publicar: {}", e.getMessage());
        }
    }
}
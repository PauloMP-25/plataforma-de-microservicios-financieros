package com.auditoria.infraestructura.mensajeria;

import com.auditoria.aplicacion.dtos.AuditoriaAccesoRequestDTO;
import com.auditoria.aplicacion.dtos.AuditoriaTransaccionalRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos de auditoría hacia RabbitMQ.
 *
 * Este componente es utilizado por los OTROS microservicios del ecosistema
 * cuando necesitan reportar eventos de seguridad o cambios de datos al
 * microservicio de auditoría de forma desacoplada y asíncrona.
 *
 * USO TÍPICO:
 *  - Microservicio-Usuario  → reportar login/logout via publicarAcceso()
 *  - Microservicio-Financiero → reportar cambios de transacciones via publicarCambio()
 *  - API Gateway → reportar intentos bloqueados via publicarAcceso()
 *
 * Routing Keys usadas:
 *   "auditoria.acceso.login"           → cola.auditoria.accesos
 *   "auditoria.acceso.logout"          → cola.auditoria.accesos
 *   "auditoria.transaccion.creacion"   → cola.auditoria.transacciones
 *   "auditoria.transaccion.actualizacion" → cola.auditoria.transacciones
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;

    // ── Routing Keys concretas (sufijos del patrón auditoria.acceso.#) ────────
    public static final String RK_ACCESO_LOGIN    = "auditoria.acceso.login";
    public static final String RK_ACCESO_LOGOUT   = "auditoria.acceso.logout";
    public static final String RK_ACCESO_FALLO    = "auditoria.acceso.fallo";

    public static final String RK_TRANSAC_CREACION      = "auditoria.transaccion.creacion";
    public static final String RK_TRANSAC_ACTUALIZACION = "auditoria.transaccion.actualizacion";
    public static final String RK_TRANSAC_ELIMINACION   = "auditoria.transaccion.eliminacion";

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLICACIÓN DE EVENTOS DE ACCESO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Publica un evento de acceso en el exchange de auditoría de forma asíncrona.
     * La anotación @Async garantiza que el hilo del microservicio llamante no
     * se bloquea esperando la confirmación del broker.
     *
     * @param dto        Datos del evento de acceso
     * @param routingKey Routing key específica (usar constantes RK_ACCESO_*)
     */
    @Async
    public void publicarAcceso(AuditoriaAccesoRequestDTO dto, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(
                    ConfiguracionRabbitMQ.EXCHANGE_AUDITORIA,
                    routingKey,
                    dto
            );
            log.debug(
                "[PUBLICADOR] Evento de acceso publicado — routingKey='{}', ip='{}', estado='{}'",
                routingKey, dto.ipOrigen(), dto.estado()
            );
        } catch (AmqpException ex) {
            // No propagamos: la auditoría es informativa, no debe bloquear el flujo principal
            log.error(
                "[PUBLICADOR] Fallo al publicar evento de acceso (no bloqueante) " +
                "— routingKey='{}', ip='{}', error='{}'",
                routingKey, dto.ipOrigen(), ex.getMessage()
            );
        }
    }

    /**
     * Atajo para publicar un intento de login fallido.
     * @param dto
     */
    @Async
    public void publicarLoginFallido(AuditoriaAccesoRequestDTO dto) {
        publicarAcceso(dto, RK_ACCESO_FALLO);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLICACIÓN DE EVENTOS TRANSACCIONALES
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Publica un evento de cambio en una entidad de negocio.
     * Garantiza trazabilidad completa para auditorías de cumplimiento (PCIDSS, SOX).
     *
     * @param dto        Datos del cambio (con valorAnterior/valorNuevo en JSON)
     * @param routingKey Routing key específica (usar constantes RK_TRANSAC_*)
     */
    @Async
    public void publicarCambio(AuditoriaTransaccionalRequestDTO dto, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(
                    ConfiguracionRabbitMQ.EXCHANGE_AUDITORIA,
                    routingKey,
                    dto
            );
            log.debug(
                "[PUBLICADOR] Cambio transaccional publicado — routingKey='{}', " +
                "entidad='{}', id='{}'",
                routingKey, dto.entidadAfectada(), dto.entidadId()
            );
        } catch (AmqpException ex) {
            log.error(
                "[PUBLICADOR] Fallo al publicar cambio transaccional (no bloqueante) " +
                "— entidad='{}', id='{}', error='{}'",
                dto.entidadAfectada(), dto.entidadId(), ex.getMessage()
            );
        }
    }
}

package com.auditoria.infraestructura.mensajeria;

import com.auditoria.aplicacion.dtos.AuditoriaAccesoRequestDTO;
import com.auditoria.aplicacion.dtos.AuditoriaTransaccionalRequestDTO;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor AMQP del Microservicio de Auditoría.
 *
 * Escucha dos colas independientes y delega el procesamiento al
 * ServicioSeguridadAuditoria. El manejo de errores sigue esta estrategia:
 *
 * ┌────────────────────────────────────────────────────────────────────┐ │ TIPO
 * DE ERROR │ ACCIÓN │
 * ├────────────────────────────────────────────────────────────────────┤ │
 * Error de negocio predecible │ Log + AmqpRejectAndDontRequeue │ │ (datos
 * inválidos, NPE, etc.) │ → Mensaje va directo a la DLQ │
 * ├────────────────────────────────────────────────────────────────────┤ │
 * Error transitorio │ RuntimeException normal │ │ (BD caída, timeout, etc.) │ →
 * Spring reencola el mensaje │ │ │ → Tras el TTL, va a la DLQ │
 * └────────────────────────────────────────────────────────────────────┘
 *
 * IMPORTANTE: AmqpRejectAndDontRequeueException es la señal explícita a Spring
 * AMQP para que NO reintente el mensaje y lo envíe a la DLQ. Usarla para
 * errores donde reintentar no tiene sentido (datos corruptos).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorAuditoria {

    private final ServicioSeguridadAuditoria servicioSeguridad;

    // ══════════════════════════════════════════════════════════════════════════
    // CONSUMIDOR 1: EVENTOS DE ACCESO
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Escucha la cola de accesos y registra el evento en la base de datos. Si
     * el estado del evento es FALLO, delega en la lógica de detección de fuerza
     * bruta del servicio.
     *
     * @param dto DTO deserializado automáticamente desde JSON por Jackson
     * @param routingKey Routing key del mensaje (inyectada del header AMQP)
     */
    // =========================================================================
    // CONSUMIDOR 1: EVENTOS DE ACCESO
    // =========================================================================
    @RabbitListener(
            queues = ConfiguracionRabbitMQ.COLA_ACCESOS,
            containerFactory = "rabbitListenerContainerFactory", // La fábrica (definida en ConfiguracionRabbitMQ)
            errorHandler = "auditoriaErrorHandler"               // El manejador de errores (ManejadorErroresRabbit)
    )

    public void consumirEventoAcceso(
            AuditoriaAccesoRequestDTO dto,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("[CONSUMIDOR-ACCESOS] Recibido: {} | IP: {}", routingKey, dto.ipOrigen());

        validarDtoAcceso(dto);

        // Si es de mensajería, el servicio ya sabe manejarlo por el contenido del DTO
        servicioSeguridad.registrarAcceso(dto);

        log.debug("[CONSUMIDOR-ACCESOS] Éxito.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONSUMIDOR 2: EVENTOS TRANSACCIONALES
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Escucha la cola de transacciones y registra el cambio de entidad. En un
     * entorno FinTech, estos mensajes son críticos: representan la trazabilidad
     * completa de modificaciones a datos financieros.
     *
     * @param dto DTO con el estado anterior/nuevo de la entidad en JSON
     * @param routingKey Routing key original del mensaje
     */
    @RabbitListener(
            queues = ConfiguracionRabbitMQ.COLA_TRANSACCIONES,
            containerFactory = "rabbitListenerContainerFactory",
            errorHandler = "auditoriaErrorHandler"
    )
    public void consumirEventoTransaccional(
            AuditoriaTransaccionalRequestDTO dto,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("[CONSUMIDOR-TRANSAC] Recibido: {} | Entidad: {}", routingKey, dto.entidadAfectada());

        validarDtoTransaccional(dto);
        servicioSeguridad.registrarCambio(dto);

        log.debug("[CONSUMIDOR-TRANSAC] Éxito.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDACIONES PRIVADAS
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Valida los campos mínimos obligatorios de un DTO de acceso. Se lanza
     * antes de llamar al servicio para detectar datos corruptos temprano y
     * evitar consultas innecesarias a la base de datos.
     */
    private void validarDtoAcceso(AuditoriaAccesoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El DTO de acceso no puede ser nulo.");
        }
        if (dto.ipOrigen() == null || dto.ipOrigen().isBlank()) {
            throw new IllegalArgumentException("La IP de origen es obligatoria.");
        }
        if (dto.estado() == null) {
            throw new IllegalArgumentException("El estado del acceso (EXITO/FALLO) es obligatorio.");
        }
    }

    /**
     * Valida los campos mínimos obligatorios de un DTO transaccional.
     */
    private void validarDtoTransaccional(AuditoriaTransaccionalRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El DTO transaccional no puede ser nulo.");
        }
        if (dto.usuarioId() == null) {
            throw new IllegalArgumentException("El usuarioId es obligatorio en el evento transaccional.");
        }
        if (dto.entidadAfectada() == null || dto.entidadAfectada().isBlank()) {
            throw new IllegalArgumentException("La entidad afectada es obligatoria.");
        }
        if (dto.entidadId() == null || dto.entidadId().isBlank()) {
            throw new IllegalArgumentException("El ID de la entidad es obligatorio.");
        }
        if (dto.servicioOrigen() == null || dto.servicioOrigen().isBlank()) {
            throw new IllegalArgumentException("El servicio de origen es obligatorio.");
        }
    }
}

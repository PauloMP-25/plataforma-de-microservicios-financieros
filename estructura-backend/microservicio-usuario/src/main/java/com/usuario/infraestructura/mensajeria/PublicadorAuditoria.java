package com.usuario.infraestructura.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.dominio.entidades.BandejaSalidaAuditoria;
import com.usuario.dominio.repositorios.RepositorioBandejaSalidaAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del publicador de auditoría para el microservicio de usuario.
 * Hereda de PublicadorEventosBase e implementa el patrón Outbox para garantizar
 * que ningún evento de auditoría (Acceso, Transaccional, General) se pierda
 * si RabbitMQ se encuentra temporalmente caído.
 */
@Component
@Slf4j
public class PublicadorAuditoria extends PublicadorEventosBase {

    private final RepositorioBandejaSalidaAuditoria repositorioOutbox;
    private final ObjectMapper objectMapper;
    private final PublicadorAmqpAsincrono publicadorAmqpAsincrono;

    public PublicadorAuditoria(
            RabbitTemplate rabbitTemplate,
            RepositorioBandejaSalidaAuditoria repositorioOutbox,
            ObjectMapper objectMapper,
            PublicadorAmqpAsincrono publicadorAmqpAsincrono) {
        super(rabbitTemplate);
        this.repositorioOutbox = repositorioOutbox;
        this.objectMapper = objectMapper;
        this.publicadorAmqpAsincrono = publicadorAmqpAsincrono;
    }

    /**
     * Publica un evento de acceso utilizando el patrón Outbox.
     * Persiste primero en la base de datos de forma transaccional,
     * e intenta publicarlo de forma inmediata en RabbitMQ.
     */
    @SuppressWarnings("null")
    @Transactional
    public void publicarAcceso(UUID usuarioId, String ipCliente, EstadoEvento estado, String detalle) {
        String correlationId = UUID.randomUUID().toString();
        
        EventoAccesoDTO evento = new EventoAccesoDTO(
                usuarioId,
                ipCliente,
                "MICROSERVICIO-USUARIO",
                        estado,
                detalle,
                LocalDateTime.now(),
                correlationId
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(evento);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] Error al serializar EventoAccesoDTO a JSON: {}", e.getMessage());
            throw new RuntimeException("Error al serializar el evento de auditoría para Outbox", e);
        }

        // 1. Guardar de forma transaccional en la Bandeja de Salida (Outbox)
        BandejaSalidaAuditoria outbox = BandejaSalidaAuditoria.builder()
                .tipoEvento("ACCESO_" + estado.name())
                .payload(payloadJson)
                .procesado(false)
                .intentos(0)
                .build();

        outbox = repositorioOutbox.save(outbox);
        log.info("[OUTBOX] Registro de acceso persistido en DB (ID: {}, Estado: {})", outbox.getId(), estado);

        // 2. Intentar la entrega asíncrona a RabbitMQ tras el commit exitoso de la
        // transacción
        final UUID outboxId = outbox.getId();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicadorAmqpAsincrono.enviarAccesoAsincrono(evento, estado, outboxId);
                        }
                    });
        } else {
            // Si por alguna razón no hay transacción activa, lo publicamos asíncronamente
            // de inmediato
            publicadorAmqpAsincrono.enviarAccesoAsincrono(evento, estado, outboxId);
        }
    }

    /**
     * Publica un evento transaccional de cambio de estado de negocio (ej:
     * actualización de plan).
     * Persiste primero en la base de datos de forma transaccional,
     * e intenta publicarlo de forma inmediata en RabbitMQ.
     */
    @SuppressWarnings("null")
    @Transactional
    public void publicarTransaccion(
            UUID usuarioId,
            UUID entidadId,
            String entidad,
            String descripcion,
            String valorAnterior,
            String valorNuevo) {

        EventoTransaccionalDTO evento = EventoTransaccionalDTO.crear(
                usuarioId,
                entidadId,
                "microservicio-usuario",
                        entidad,
                descripcion,
                valorAnterior,
                valorNuevo);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(evento);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] Error al serializar EventoTransaccionalDTO a JSON: {}", e.getMessage());
            throw new RuntimeException("Error al serializar el evento transaccional para Outbox", e);
        }

        // 1. Guardar de forma transaccional en la Bandeja de Salida (Outbox)
        BandejaSalidaAuditoria outbox = BandejaSalidaAuditoria.builder()
                .tipoEvento("TRANSACCIONAL")
                .payload(payloadJson)
                .procesado(false)
                .intentos(0)
                .build();

        outbox = repositorioOutbox.save(outbox);
        log.info("[OUTBOX] Registro transaccional persistido en DB (ID: {}, Entidad: {})", outbox.getId(), entidad);

        // 2. Intentar la entrega asíncrona a RabbitMQ tras el commit exitoso de la
        // transacción
        final UUID outboxId = outbox.getId();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicadorAmqpAsincrono.enviarTransaccionAsincrono(evento, entidad, outboxId);
                        }
                    });
        } else {
            // Si por alguna razón no hay transacción activa, lo publicamos asíncronamente
            // de inmediato
            publicadorAmqpAsincrono.enviarTransaccionAsincrono(evento, entidad, outboxId);
        }
    }

    /**
     * Publica un evento de auditoría general (ej: OTP, recuperaciones, etc.).
     * Persiste primero en la base de datos de forma transaccional,
     * e intenta publicarlo de forma inmediata en RabbitMQ.
     */
    @SuppressWarnings("null")
    @Transactional
    public void publicarEventoGeneral(
            UUID usuarioId,
            String accion,
            String modulo,
            String ipOrigen,
            String detalles) {

        String moduloFinal = modulo.toLowerCase().startsWith("usuario-servicio")
                ? modulo.toLowerCase()
                : "usuario-servicio-" + modulo.toLowerCase();

        EventoAuditoriaDTO evento = EventoAuditoriaDTO.crear(
                usuarioId,
                accion,
                moduloFinal,
                        ipOrigen,
                detalles);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(evento);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] Error al serializar EventoAuditoriaDTO a JSON: {}", e.getMessage());
            throw new RuntimeException("Error al serializar el evento de auditoría general para Outbox", e);
        }

        // 1. Guardar de forma transaccional en la Bandeja de Salida (Outbox)
        BandejaSalidaAuditoria outbox = BandejaSalidaAuditoria.builder()
                .tipoEvento("AUDITORIA_GENERAL")
                .payload(payloadJson)
                .procesado(false)
                .intentos(0)
                .build();

        outbox = repositorioOutbox.save(outbox);
        log.info("[OUTBOX] Registro de auditoría general persistido en DB (ID: {}, Accion: {})", outbox.getId(),
                accion);

        // 2. Intentar la entrega asíncrona a RabbitMQ tras el commit exitoso de la
        // transacción
        final UUID outboxId = outbox.getId();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicadorAmqpAsincrono.enviarEventoGeneralAsincrono(evento, accion, outboxId);
                        }
                    });
        } else {
            // Si por alguna razón no hay transacción activa, lo publicamos asíncronamente
            // de inmediato
            publicadorAmqpAsincrono.enviarEventoGeneralAsincrono(evento, accion, outboxId);
        }
    }

    /**
     * Envía una solicitud de generación de OTP con los headers necesarios para el
     * motor de plantillas.
     */
    public void publicarSolicitudOtp(SolicitudGenerarOtp dto) {
        log.info("[RABBITMQ] Solicitud de OTP encolada para: {} (Tipo: {})", dto.usuarioId(), dto.tipo());

        Map<String, Object> headers = Map.of(
                "x-otp-proposito", dto.proposito().name(),
                "x-otp-tipo", dto.tipo().name()
        );

        super.enviarConHeaders(NombresExchange.MENSAJERIA, RoutingKeys.MENSAJERIA_OTP_GENERAR, dto, headers);
    }

    /**
     * Envía una notificación de seguridad (ej. aviso de cambio de contraseña) a
     * ms-mensajeria.
     */
    public void publicarNotificacionSeguridad(UUID usuarioId, String correo, String accion) {
        log.info("[RABBITMQ] Publicando notificación de seguridad: {} para {}", accion, usuarioId);
        
        Map<String, Object> mensaje = Map.of(
                "usuarioId", usuarioId.toString(),
                "correo", correo,
                "accion", accion,
                "fecha", LocalDateTime.now().toString()
        );

        Map<String, Object> headers = Map.of("x-evento-tipo", "NOTIFICACION_SEGURIDAD");
        
        super.enviarConHeaders(NombresExchange.MENSAJERIA, "notificacion.seguridad", mensaje, headers);
    }
}

package com.cliente.infraestructura.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.cliente.dominio.entidades.BandejaSalidaAuditoria;
import com.cliente.dominio.repositorios.RepositorioBandejaSalidaAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.UUID;

/**
 * Publicador especializado de eventos para el Microservicio de Cliente.
 * Implementa el patrón Outbox para garantizar la entrega confiable de eventos.
 * 
 * @author Paulo Moron
 * @version 1.6
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
     * Reporta un evento de forma asíncrona usando Outbox.
     */
    @Transactional
    public void publicarEventoExitoso(EventoAuditoriaDTO dto) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] Error al serializar EventoAuditoriaDTO a JSON: {}", e.getMessage());
            throw new RuntimeException("Error al serializar el evento de auditoría para Outbox", e);
        }

        BandejaSalidaAuditoria outbox = BandejaSalidaAuditoria.builder()
                .tipoEvento("AUDITORIA_GENERAL")
                .payload(payloadJson)
                .procesado(false)
                .intentos(0)
                .build();

        outbox = repositorioOutbox.save(outbox);
        log.info("[OUTBOX] Registro de auditoría persistido en DB (ID: {}, Acción: {})", outbox.getId(), dto.accion());

        final UUID outboxId = outbox.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicadorAmqpAsincrono.enviarEventoGeneralAsincrono(dto, "exito", outboxId);
                        }
                    });
        } else {
            publicadorAmqpAsincrono.enviarEventoGeneralAsincrono(dto, "exito", outboxId);
        }
    }

    /**
     * Reporta un evento transaccional de forma asíncrona usando Outbox.
     */
    @Transactional
    public void publicarTransaccionExitosa(EventoTransaccionalDTO dto) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] Error al serializar EventoTransaccionalDTO a JSON: {}", e.getMessage());
            throw new RuntimeException("Error al serializar el evento transaccional para Outbox", e);
        }

        BandejaSalidaAuditoria outbox = BandejaSalidaAuditoria.builder()
                .tipoEvento("TRANSACCIONAL")
                .payload(payloadJson)
                .procesado(false)
                .intentos(0)
                .build();

        outbox = repositorioOutbox.save(outbox);
        log.info("[OUTBOX] Registro transaccional persistido en DB (ID: {}, Entidad: {})", outbox.getId(),
                dto.entidadAfectada());

        final UUID outboxId = outbox.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publicadorAmqpAsincrono.enviarTransaccionAsincrono(dto, dto.entidadAfectada(), outboxId);
                        }
                    });
        } else {
            publicadorAmqpAsincrono.enviarTransaccionAsincrono(dto, dto.entidadAfectada(), outboxId);
        }
    }
}

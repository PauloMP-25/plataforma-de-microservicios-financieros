package com.cliente.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.cliente.dominio.entidades.BandejaSalidaAuditoria;
import com.cliente.dominio.repositorios.RepositorioBandejaSalidaAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Componente que delega de forma asíncrona la publicación de mensajes en RabbitMQ.
 * <p>
 * Se ejecuta tras el commit exitoso de la transacción principal para garantizar la consistencia
 * y evitar problemas de aislamiento de base de datos.
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAmqpAsincrono {

    private final PublicadorEventosBase publicadorEventosBase;
    private final RepositorioBandejaSalidaAuditoria repositorioOutbox;

    /**
     * Envía de forma asíncrona un evento transaccional a RabbitMQ y marca el outbox como procesado.
     */
    @Async("taskExecutor")
    @Transactional
    public void enviarTransaccionAsincrono(EventoTransaccionalDTO evento, String entidad, UUID outboxId) {
        try {
            publicadorEventosBase.publicarTransaccion(evento, entidad);
            marcarComoProcesado(outboxId);
            log.info("[ASYNC-AMQP] Evento transaccional enviado a RabbitMQ y confirmado en DB para ID: {}", outboxId);
        } catch (Exception e) {
            log.warn("[ASYNC-AMQP-ERROR] Falló envío asíncrono inmediato para Transacción. ID Outbox: {}. Error: {}", outboxId, e.getMessage());
        }
    }

    /**
     * Envía de forma asíncrona un evento general a RabbitMQ y marca el outbox como procesado.
     */
    @Async("taskExecutor")
    @Transactional
    public void enviarEventoGeneralAsincrono(EventoAuditoriaDTO evento, String accion, UUID outboxId) {
        try {
            publicadorEventosBase.publicarEvento(evento, accion);
            marcarComoProcesado(outboxId);
            log.info("[ASYNC-AMQP] Evento general enviado a RabbitMQ y confirmado en DB para ID: {}", outboxId);
        } catch (Exception e) {
            log.warn("[ASYNC-AMQP-ERROR] Falló envío asíncrono inmediato para Evento General. ID Outbox: {}. Error: {}", outboxId, e.getMessage());
        }
    }

    private void marcarComoProcesado(UUID outboxId) {
        BandejaSalidaAuditoria outbox = repositorioOutbox.findById(outboxId).orElse(null);
        if (outbox != null) {
            outbox.setProcesado(true);
            outbox.setFechaProceso(LocalDateTime.now());
            repositorioOutbox.save(outbox);
        }
    }
}

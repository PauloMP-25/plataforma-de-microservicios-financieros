package com.mensajeria.infraestructura.tareas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.mensajeria.dominio.entidades.BandejaSalidaMensajeria;
import com.mensajeria.dominio.repositorios.RepositorioBandejaSalidaMensajeria;
import com.mensajeria.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tarea programada para reintentar el envío de eventos de la Bandeja de Salida
 * (Outbox Pattern) hacia RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TareaReintentadorAuditoria {

    private final RepositorioBandejaSalidaMensajeria outboxRepository;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${mensajeria.outbox.reintento-ms:60000}")
    @Transactional
    public void procesarEventosPendientes() {
        List<BandejaSalidaMensajeria> pendientes = outboxRepository.findByProcesadoFalseAndIntentosLessThan(5);
        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[OUTBOX-SCHEDULER] Procesando {} eventos de auditoría pendientes", pendientes.size());

        for (BandejaSalidaMensajeria outbox : pendientes) {
            try {
                outbox.setIntentos(outbox.getIntentos() + 1);
                
                EventoAuditoriaDTO dto = objectMapper.readValue(outbox.getPayload(), EventoAuditoriaDTO.class);
                
                // Usamos el publicador base para enviar sin volver a guardar en la bandeja
                publicadorAuditoria.publicarEvento(dto, outbox.getTipoEvento());

                outbox.setProcesado(true);
                log.debug("[OUTBOX-SCHEDULER] Evento {} enviado exitosamente", outbox.getId());
            } catch (Exception e) {
                log.error("[OUTBOX-SCHEDULER] Fallo al reintentar evento {}: {}", outbox.getId(), e.getMessage());
            }
        }
        
        outboxRepository.saveAll(pendientes);
    }
}

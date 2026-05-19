package com.cliente.infraestructura.tareas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.cliente.dominio.entidades.BandejaSalidaAuditoria;
import com.cliente.dominio.repositorios.RepositorioBandejaSalidaAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea programada (Scheduler) que reintenta publicar todos los eventos de auditoría
 * (Transacciones y Auditorías Generales) pendientes en la bandeja de salida (Outbox) del cliente.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareaReintentadorAuditoria {

    private static final int MAX_INTENTOS = 5;

    private final RepositorioBandejaSalidaAuditoria repositorioBandejaSalida;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Se ejecuta periódicamente para procesar y reenviar mensajes de auditoría que quedaron pendientes de envío.
     */
    @Scheduled(fixedDelayString = "${app.outbox.reintento-ms:60000}")
    @Transactional
    public void reintentarEventosPendientes() {
        List<BandejaSalidaAuditoria> pendientes = repositorioBandejaSalida
                .findByProcesadoFalseAndIntentosLessThan(MAX_INTENTOS);

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[OUTBOX-AUDITORIA] Procesando {} eventos de auditoría pendientes de entrega...", pendientes.size());

        for (BandejaSalidaAuditoria entrada : pendientes) {
            try {
                String payload = entrada.getPayload();
                String tipo = entrada.getTipoEvento();

                if ("TRANSACCIONAL".equals(tipo)) {
                    // 1. Procesar Eventos Transaccionales
                    EventoTransaccionalDTO evento = objectMapper.readValue(payload, EventoTransaccionalDTO.class);
                    String routingKey = "auditoria.transaccion." + evento.entidadAfectada().toLowerCase();
                    rabbitTemplate.convertAndSend(NombresExchange.AUDITORIA, routingKey, evento);
                    
                } else if ("AUDITORIA_GENERAL".equals(tipo)) {
                    // 2. Procesar Eventos de Auditoría General
                    EventoAuditoriaDTO evento = objectMapper.readValue(payload, EventoAuditoriaDTO.class);
                    String routingKey = "auditoria.evento" + evento.accion().toLowerCase();
                    rabbitTemplate.convertAndSend(NombresExchange.AUDITORIA, routingKey, evento);
                }

                entrada.setProcesado(true);
                entrada.setFechaProceso(LocalDateTime.now());
                log.info("[OUTBOX-AUDITORIA-SUCCESS] Evento de tipo {} (ID: {}) enviado exitosamente tras {} intentos.", 
                        tipo, entrada.getId(), entrada.getIntentos());

            } catch (Exception e) {
                entrada.setIntentos(entrada.getIntentos() + 1);
                log.error("[OUTBOX-AUDITORIA-FAILED] Intento fallido #{} para evento {} de tipo {}: {}", 
                        entrada.getIntentos(), entrada.getId(), entrada.getTipoEvento(), e.getMessage());
            }
            
            repositorioBandejaSalida.save(entrada);
        }
    }
}

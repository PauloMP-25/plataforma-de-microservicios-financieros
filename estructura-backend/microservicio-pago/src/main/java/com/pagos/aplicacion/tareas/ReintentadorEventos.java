package com.pagos.aplicacion.tareas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.pagos.dominio.entidades.BandejaSalida;
import com.pagos.dominio.repositorios.RepositorioBandejaSalida;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea programada que reintenta publicar eventos pendientes en la bandeja de salida.
 * Garantiza la entrega final de mensajes (Eventual Consistency).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReintentadorEventos {

    private static final int MAX_INTENTOS = 5;

    private final RepositorioBandejaSalida repositorioBandejaSalida;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Se ejecuta periódicamente para procesar eventos no enviados.
     */
    @Scheduled(fixedDelayString = "${app.outbox.reintento-ms:60000}")
    @Transactional
    public void reintentarEventosPendientes() {
        List<BandejaSalida> pendientes = repositorioBandejaSalida
            .findByProcesadoFalseAndIntentosLessThan(MAX_INTENTOS);

        if (pendientes.isEmpty()) return;

        log.info("[OUTBOX-SCHEDULER] Procesando {} eventos pendientes...", pendientes.size());

        for (BandejaSalida entrada : pendientes) {
            try {
                // Deserializar para validar y enviar como objeto
                EventoPagoExitosoDTO evento = objectMapper.readValue(
                    entrada.getPayload(), EventoPagoExitosoDTO.class);

                // Reintentar publicación
                rabbitTemplate.convertAndSend(
                    NombresExchange.PAGOS,
                    entrada.getTipoEvento(),
                    evento
                );

                entrada.setProcesado(true);
                entrada.setFechaProceso(LocalDateTime.now());
                log.info("[OUTBOX-SUCCESS] Evento {} enviado tras {} intentos.", entrada.getId(), entrada.getIntentos());

            } catch (Exception e) {
                entrada.setIntentos(entrada.getIntentos() + 1);
                log.error("[OUTBOX-RETRY-FAILED] Error en intento {} para evento {}: {}", 
                    entrada.getIntentos(), entrada.getId(), e.getMessage());
            }
            repositorioBandejaSalida.save(entrada);
        }
    }
}

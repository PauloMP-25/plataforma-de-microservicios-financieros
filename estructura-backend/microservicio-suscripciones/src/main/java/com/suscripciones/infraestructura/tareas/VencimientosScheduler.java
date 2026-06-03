package com.suscripciones.infraestructura.tareas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suscripciones.dominio.entidades.BandejaSalida;
import com.suscripciones.dominio.entidades.Suscripcion;
import com.suscripciones.dominio.repositorios.BandejaSalidaRepository;
import com.suscripciones.dominio.repositorios.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduler encargado de verificar periódicamente el vencimiento de las suscripciones.
 * Si una suscripción activa supera su fecha de vencimiento, cambia su estado a VENCIDA
 * y genera un evento en el outbox para alertar al usuario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VencimientosScheduler {

    private final SuscripcionRepository suscripcionRepository;
    private final BandejaSalidaRepository bandejaSalidaRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${luka.scheduler.vencimientos.cron:0 0 0 * * ?}") // Ejecuta todos los días a medianoche por defecto
    @SchedulerLock(name = "procesarVencimientosYRecordatorios", lockAtMostFor = "10m", lockAtLeastFor = "5m")
    @Transactional
    public void procesarVencimientosYRecordatorios() {
        log.info("Iniciando verificación programada de vencimientos de suscripciones...");
        LocalDate hoy = LocalDate.now();

        List<Suscripcion> activas = suscripcionRepository.findByEstado("ACTIVA");
        List<Suscripcion> vencidas = activas.stream()
                .filter(s -> s.getFechaVencimiento() != null && s.getFechaVencimiento().isBefore(hoy))
                .collect(Collectors.toList());

        if (vencidas.isEmpty()) {
            log.info("No se encontraron suscripciones activas vencidas.");
            return;
        }

        log.info("Se encontraron {} suscripciones vencidas. Actualizando estados...", vencidas.size());

        for (Suscripcion s : vencidas) {
            s.setEstado("VENCIDA");
            suscripcionRepository.save(s);
            log.info("Suscripción '{}' del usuario {} marcada como VENCIDA (Vencimiento: {})",
                    s.getNombre(), s.getUsuarioId(), s.getFechaVencimiento());

            // Registrar evento de outbox para alertar al usuario (ej. envío de email)
            try {
                Map<String, Object> payloadMap = Map.of(
                        "suscripcionId", s.getId().toString(),
                        "usuarioId", s.getUsuarioId().toString(),
                        "nombre", s.getNombre(),
                        "fechaVencimiento", s.getFechaVencimiento().toString()
                );
                String jsonPayload = objectMapper.writeValueAsString(payloadMap);
                
                BandejaSalida outbox = new BandejaSalida("EVENTO_SUSCRIPCION_VENCIDA", jsonPayload);
                bandejaSalidaRepository.save(outbox);
                log.info("Evento 'EVENTO_SUSCRIPCION_VENCIDA' registrado en Outbox para suscripción ID: {}", s.getId());
                
            } catch (JsonProcessingException e) {
                log.error("Error al serializar el evento de vencimiento para outbox (Suscripción ID: {})", s.getId(), e);
            }
        }
    }
}

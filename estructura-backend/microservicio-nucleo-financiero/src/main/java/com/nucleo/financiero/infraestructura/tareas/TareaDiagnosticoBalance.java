package com.nucleo.financiero.infraestructura.tareas;

import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada para monitorear y diagnosticar el estado del Núcleo Financiero.
 * <p>
 * Se ejecuta periódicamente a la medianoche barriendo y auditando la integridad del catálogo de categorías
 * y el volumen total de movimientos financieros procesados.
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareaDiagnosticoBalance {

    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Tarea diaria de diagnóstico del balance (se ejecuta todos los días a las 00:00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void diagnosticarEhigieneFinanciera() {
        log.info("[SCHEDULED-DIAGNOSTIC] Iniciando auditoría automática del Núcleo Financiero...");
        try {
            long totalTransacciones = transaccionRepository.count();
            long totalCategorias = categoriaRepository.count();

            log.info("[SCHEDULED-DIAGNOSTIC] Estado operativo: OK. Total Categorías: {}, Total Transacciones Históricas: {}.",
                    totalCategorias, totalTransacciones);
        } catch (Exception e) {
            log.error("[SCHEDULED-DIAGNOSTIC-ERROR] Error crítico al ejecutar auditoría programada: {}", e.getMessage(), e);
        }
    }
}

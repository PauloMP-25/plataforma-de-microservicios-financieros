package com.cliente.infraestructura.tareas;

import com.cliente.dominio.entidades.LimiteGasto;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tarea programada para desactivar automáticamente los límites de gasto vencidos.
 * Garantiza la consistencia e higiene de los límites activos en el sistema.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareaDesactivacionLimitesVencidos {

    private final LimiteGastoRepositorio repositorio;

    /**
     * Se ejecuta todos los días a la medianoche para desactivar los límites de gasto
     * cuya fecha de fin ya haya vencido.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void desactivarLimitesVencidos() {
        log.info("[TAREA-PROGRAMADA] Iniciando barrido diario de límites de gasto vencidos...");
        
        List<LimiteGasto> todos = repositorio.findAll();
        int desactivados = 0;

        for (LimiteGasto limite : todos) {
            if (limite.isActivo() && limite.estaVencido()) {
                limite.setActivo(false);
                repositorio.save(limite);
                desactivados++;
                log.info("[TAREA-PROGRAMADA] Límite global ID {} del usuario {} marcado como inactivo (vencido el {}).",
                        limite.getId(), limite.getUsuarioId(), limite.getFechaFin());
            }
        }

        log.info("[TAREA-PROGRAMADA] Barrido completado. Total de límites vencidos desactivados: {}", desactivados);
    }
}

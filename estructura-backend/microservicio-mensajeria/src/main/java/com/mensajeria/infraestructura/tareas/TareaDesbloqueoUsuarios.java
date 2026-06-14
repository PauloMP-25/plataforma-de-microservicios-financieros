package com.mensajeria.infraestructura.tareas;

import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tarea programada para el desbloqueo masivo y oportuno de usuarios 
 * cuyos bloqueos por intentos fallidos hayan expirado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TareaDesbloqueoUsuarios {

    private final IntentoValidacionRepository intentoRepository;

    /**
     * Se ejecuta periódicamente para desbloquear usuarios con castigos vencidos.
     * Frecuencia configurable mediante properties (por defecto cada 15 minutos).
     */
    @Scheduled(fixedDelayString = "${mensajeria.desbloqueo.intervalo-ms:900000}")
    @Transactional
    public void ejecutarDesbloqueoExpirados() {
        log.debug("[SCHEDULER] Iniciando tarea programada de desbloqueo de usuarios...");
        LocalDateTime ahora = LocalDateTime.now();
        int desbloqueados = intentoRepository.desbloquearUsuariosExpirados(ahora);
        if (desbloqueados > 0) {
            log.info("[SCHEDULER] Desbloqueo completado. Usuarios liberados de castigo: {}", desbloqueados);
        }
    }
}

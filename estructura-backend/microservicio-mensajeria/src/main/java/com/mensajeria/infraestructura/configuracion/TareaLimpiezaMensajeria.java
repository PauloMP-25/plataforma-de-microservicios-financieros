package com.mensajeria.infraestructura.configuracion;

import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TareaLimpiezaMensajeria {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;

    /**
     * Se ejecuta cada 24 horas para ahorrar memoria.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Se ejecuta a las 2:00 AM todos los días
    @Transactional
    public void ejecutarLimpiezaProfunda() {
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Limpiar códigos (Usados o expirados)
        int codigosBorrados = codigoRepository.eliminarCodigosObsoletos(ahora);

        // 2. Limpiar registros de intentos inactivos (más de 7 días sin fallos)
        int intentosBorrados = intentoRepository.purgarRegistrosInactivos(ahora.minusDays(7));

        log.info("--- REPORTE DE LIMPIEZA MENSAJERÍA ---");
        log.info("Códigos OTP eliminados: {}", codigosBorrados);
        log.info("Registros de intentos purgados: {}", intentosBorrados);
        log.info("--------------------------------------");
    }
}

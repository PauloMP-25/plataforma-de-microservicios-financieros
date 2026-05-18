package com.auditoria.infraestructura.tareas;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaAcceso;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada para la limpieza y mantenimiento periódico de registros
 * de auditoría y bloqueos de seguridad.
 * <p>
 * Centraliza la programación de tareas programadas cumpliendo con el
 * principio de responsabilidad única (SRP), delegando la ejecución a los
 * respectivos servicios.
 * </p>
 * 
 * @author Paulo Moron
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareaLimpiezaAuditoria {

    private final ServicioAuditoriaAcceso servicioAcceso;
    private final ServicioSeguridadAuditoria servicioSeguridad;

    @Value("${auditoria.retencion.dias:30}")
    private int diasRetencion;

    /**
     * Tarea programada para limpiar las IPs cuyos bloqueos temporales han expirado.
     * Se ejecuta cada hora.
     */
    @Scheduled(fixedRate = 3600000)
    public void limpiarBloqueosExpirados() {
        log.info("[TAREA-PROGRAMADA] Iniciando limpieza periódica de bloqueos de IP expirados...");
        try {
            servicioSeguridad.limpiarBloqueosExpirados();
        } catch (Exception e) {
            log.error("[TAREA-PROGRAMADA] Error al limpiar bloqueos expirados: {}", e.getMessage());
        }
    }

    /**
     * Tarea programada para purgar de manera automática los accesos antiguos.
     * Se ejecuta de forma diaria (cada 24 horas).
     */
    @Scheduled(fixedRate = 86400000)
    public void limpiarAccesosAntiguos() {
        log.info("[TAREA-PROGRAMADA] Iniciando purga programada de registros de acceso anteriores a {} días...", diasRetencion);
        try {
            servicioAcceso.limpiarRegistrosAntiguos(diasRetencion);
        } catch (Exception e) {
            log.error("[TAREA-PROGRAMADA] Error al purgar accesos antiguos: {}", e.getMessage());
        }
    }
}

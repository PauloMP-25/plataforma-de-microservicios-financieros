package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;

/**
 * Interfaz encargada de la política de seguridad y defensa perimetral.
 * <p>
 * Define los métodos para evaluar ataques de fuerza bruta, gestionar el bloqueo
 * de IPs y verificar el estado de las mismas frente a la lista negra.
 * </p>
 * 
 * @author Paulo Moron
 */
public interface ServicioSeguridadAuditoria {

    /**
     * Evalúa si una IP debe ser bloqueada tras un intento fallido de acceso.
     * 
     * @param ipOrigen Dirección IP a evaluar.
     */
    void verificarIntentoFallido(String ipOrigen);

    /**
     * Comprueba si una IP está habilitada para realizar peticiones.
     * 
     * @param ip Dirección IP a verificar.
     * @return DTO con el estado de la IP (LIBRE/BLOQUEADA).
     */
    RespuestaVerificacionIpDTO verificarEstadoIp(String ip);

    /**
     * Tarea de mantenimiento para remover bloqueos cuya fecha de expiración ha pasado.
     */
    void limpiarBloqueosExpirados();
}

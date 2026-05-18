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

    /**
     * Recupera una lista paginada de todos los bloqueos de IP (históricos y activos).
     * 
     * @param paginacion Datos de paginación.
     * @return Página de registros de lista negra.
     */
    org.springframework.data.domain.Page<com.auditoria.dominio.entidades.ListaNegraIp> listarBloqueos(org.springframework.data.domain.Pageable paginacion);

    /**
     * Registra un bloqueo manual para una dirección IP específica.
     * 
     * @param ip        Dirección IP a bloquear.
     * @param motivo    Razón del bloqueo.
     * @param minutos   Duración del bloqueo en minutos (0 o negativo para bloqueo indefinido).
     */
    void bloquearIpManualmente(String ip, String motivo, int minutos);

    /**
     * Remueve manualmente cualquier bloqueo activo para una IP específica.
     * 
     * @param ip Dirección IP a desbloquear.
     */
    void desbloquearIpManualmente(String ip);
}

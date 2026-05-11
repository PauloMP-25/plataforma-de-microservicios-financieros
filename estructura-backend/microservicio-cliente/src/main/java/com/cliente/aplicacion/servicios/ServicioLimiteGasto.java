package com.cliente.aplicacion.servicios;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cliente.aplicacion.dtos.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.SolicitudLimiteGasto;
import com.cliente.dominio.entidades.LimiteGasto;

/**
 * Interfaz de servicio para la gestion del limite de gasto global.
 * <p>
 * Se encarga la creacion, consulta, modificacion, eliminacion del limite de
 * gasto global.
 * </p>
 *
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioLimiteGasto {

    /**
     * Crea un nuevo límite de gasto para una categoría. Falla con 409 si ya
     * existe un límite para la misma categoría.
     *
     * @param usuarioIdToken ID del usuario autenticado
     * @param solicitud DTO con datos del límite
     * @param ipOrigen IP del cliente
     * @return RespuestaLimiteGasto con el límite creado
     */
    public RespuestaLimiteGasto crear(UUID usuarioIdToken,
            SolicitudLimiteGasto solicitud,
            String ipOrigen);

    /**
     * Actualiza el límite global ACTIVO.
     *
     * @param usuarioId ID del usuario
     * @param solicitud DTO con datos a actualizar
     * @param ip IP del cliente
     * @return RespuestaLimiteGasto con el límite actualizado
     */
    public RespuestaLimiteGasto actualizar(UUID usuarioId, SolicitudLimiteGasto solicitud, String ip);

    /**
     * Lista todos los límites del usuario, ordenados alfabéticamente
     *
     * @param usuarioId ID del usuario
     * @return Lista de RespuestaLimiteGasto
     */
    public List<RespuestaLimiteGasto> listarHistorial(UUID usuarioId);

    /**
     * Obtiene el límite activo del usuario.
     * 
     * @param usuarioId ID del usuario
     * @return RespuestaLimiteGasto DTO de respuesta
     */
    public RespuestaLimiteGasto obtenerActivo(UUID usuarioId);

    /**
     * Desactiva (eliminación lógica) el límite global actual.
     *
     * @param usuarioId ID del usuario
     * @param ip IP del cliente
     */
    public void eliminar(UUID usuarioId, String ip);

    /**
     * Evalúa el gasto TOTAL del usuario contra su límite global único.
     *
     * @param usuarioId ID del usuario
     * @param gastoTotalActual Gasto total actual
     * @param ipOrigen IP del cliente
     * @return true si se notificó el límite, false en caso contrario
     */
    public boolean evaluarYNotificarLimiteGlobal(UUID usuarioId, BigDecimal 
            gastoTotalActual, String ipOrigen);

    /**
     * Convierte una entidad al DTO de respuesta.
     *
     * @param limite Información del cliente
     * @return RespuestaLimiteGasto DTO de respuesta.
     */
    public RespuestaLimiteGasto convertirADTO(LimiteGasto limite);
}

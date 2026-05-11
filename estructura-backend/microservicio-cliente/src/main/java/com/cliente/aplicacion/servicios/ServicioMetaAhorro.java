package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.SolicitudMetaAhorro;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestión de metas de ahorro.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface ServicioMetaAhorro {

    /**
     * Crea una nueva meta de ahorro para el usuario.
     *
     * @param usuarioIdToken ID del usuario autenticado.
     * @param solicitud      DTO con los datos de la meta a crear.
     * @param ipOrigen       IP de origen del cliente.
     * @return RespuestaMetaAhorro con la meta creada.
     */
    RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen);

    /**
     * Actualiza el progreso (monto actual) de una meta existente.
     *
     * @param metaId           ID de la meta a actualizar.
     * @param usuarioIdToken   ID del usuario autenticado.
     * @param nuevoMontoActual Nuevo monto ahorrado hasta el momento.
     * @param ipOrigen         IP de origen del cliente.
     * @return RespuestaMetaAhorro con la meta actualizada.
     */
    RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken, BigDecimal nuevoMontoActual, String ipOrigen);

    /**
     * Lista todas las metas del usuario (activas e inactivas).
     *
     * @param usuarioIdToken ID del usuario autenticado.
     * @return Lista de RespuestaMetaAhorro.
     */
    List<RespuestaMetaAhorro> listar(UUID usuarioIdToken);

    /**
     * Lista solo las metas activas (no completadas), ordenadas por fecha límite.
     *
     * @param usuarioIdToken ID del usuario autenticado.
     * @return Lista de RespuestaMetaAhorro con las metas activas.
     */
    List<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken);

    /**
     * Consulta una meta específica validando que pertenece al usuario.
     *
     * @param metaId         ID de la meta a consultar.
     * @param usuarioIdToken ID del usuario autenticado.
     * @return RespuestaMetaAhorro con la meta consultada.
     */
    RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken);

    /**
     * Elimina una meta de ahorro del usuario.
     *
     * @param metaId         ID de la meta a eliminar.
     * @param usuarioIdToken ID del usuario autenticado.
     * @param ipOrigen       IP de origen del cliente.
     */
    void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen);
}

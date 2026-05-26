package com.cliente.aplicacion.puertos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudLimiteGasto;

/**
 * Interfaz de puerto para la gestión del límite de gasto global.
 *
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioLimiteGasto {

    RespuestaLimiteGasto crear(UUID usuarioIdToken, SolicitudLimiteGasto solicitud, String ipOrigen);

    RespuestaLimiteGasto actualizar(UUID usuarioId, SolicitudLimiteGasto solicitud, String ip);

    List<RespuestaLimiteGasto> listarHistorial(UUID usuarioId);

    RespuestaLimiteGasto obtenerActivo(UUID usuarioId);

    void eliminar(UUID usuarioId, String ip);

    boolean evaluarYNotificarLimiteGlobal(UUID usuarioId, BigDecimal gastoTotalActual, String ipOrigen);

    /**
     * Consulta interna del límite activo sin validación de JWT (uso para Facade).
     */
    RespuestaLimiteGasto obtenerActivoInterno(UUID usuarioId);
}

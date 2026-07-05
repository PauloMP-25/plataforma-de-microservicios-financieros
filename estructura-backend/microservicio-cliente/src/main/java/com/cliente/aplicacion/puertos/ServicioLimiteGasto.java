package com.cliente.aplicacion.puertos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudLimiteGasto;

/**
 * Interfaz de puerto para la gestión del límite de gasto global.
 *
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

    /**
     * Verifica si el límite de gasto activo del usuario ha vencido y,
     * en tal caso, lo desactiva automáticamente.
     * <p>
     * Este método es invocado de forma automática durante el evento de login exitoso.
     * Es idempotente: si no hay límite activo o si el límite vigente no ha expirado,
     * retorna sin realizar ningún cambio.
     * </p>
     *
     * @param usuarioId UUID del usuario cuyo límite se debe verificar.
     */
    void verificarYDesactivarSiVencido(UUID usuarioId);
}

package com.cliente.aplicacion.puertos;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de puerto para la gestión de metas de ahorro.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface ServicioMetaAhorro {

    RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen);

    RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken, BigDecimal nuevoMontoActual, String ipOrigen);

    List<RespuestaMetaAhorro> listar(UUID usuarioIdToken);

    List<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken);

    RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken);

    void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen);

    List<RespuestaMetaAhorro> buscar(UUID usuarioIdToken, Boolean completada, java.time.LocalDate venceAntes, Double progresoBajo);

    /**
     * Consulta interna del listado de metas sin validación de JWT (uso para Facade).
     */
    List<RespuestaMetaAhorro> listarInterno(UUID usuarioId);
}

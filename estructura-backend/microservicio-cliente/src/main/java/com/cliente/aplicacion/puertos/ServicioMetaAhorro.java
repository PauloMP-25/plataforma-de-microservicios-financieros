package com.cliente.aplicacion.puertos;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import com.libreria.comun.respuesta.Paginacion;

/**
 * Interfaz de puerto para la gestión de metas de ahorro.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface ServicioMetaAhorro {

    RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen);

    RespuestaMetaAhorro actualizarMeta(UUID metaId, UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen);

    RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken, BigDecimal nuevoMontoActual, String ipOrigen);

    Paginacion<RespuestaMetaAhorro> listar(UUID usuarioIdToken, Pageable pageable);

    Paginacion<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken, Pageable pageable);

    RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken);

    void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen);

    /**
     * Consulta interna del listado de metas sin validación de JWT (uso para Facade).
     */
    List<RespuestaMetaAhorro> listarInterno(UUID usuarioId);
}

package com.cliente.aplicacion.puertos;

import com.cliente.aplicacion.dtos.respuestas.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudPerfilFinanciero;
import java.util.UUID;

/**
 * Interfaz de puerto para el perfil financiero del cliente.
 *
 * @version 1.1.0
 */
public interface ServicioPerfilFinanciero {

    /**
     * Crea o actualiza el perfil financiero del usuario (upsert).
     */
    RespuestaPerfilFinanciero guardarOActualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudPerfilFinanciero solicitud, String ipOrigen);

    RespuestaPerfilFinanciero consultar(UUID usuarioIdRuta, UUID usuarioIdToken);

    /**
     * Consulta interna del perfil financiero sin validación de JWT (uso para Facade).
     */
    RespuestaPerfilFinanciero consultarInterno(UUID usuarioId);
}

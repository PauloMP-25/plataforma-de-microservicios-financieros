package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.SolicitudPerfilFinanciero;
import java.util.UUID;

/**
 * Interfaz de servicio para el perfil financiero del cliente.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface ServicioPerfilFinanciero {

    /**
     * Crea o actualiza el perfil financiero del usuario (upsert).
     *
     * @param usuarioIdRuta  ID del usuario en la ruta.
     * @param usuarioIdToken ID del usuario autenticado vía token.
     * @param solicitud      DTO con los datos financieros a guardar.
     * @param ipOrigen       IP de origen del cliente.
     * @return RespuestaPerfilFinanciero con el perfil guardado o actualizado.
     */
    RespuestaPerfilFinanciero guardarOActualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudPerfilFinanciero solicitud, String ipOrigen);

    /**
     * Consulta el perfil financiero del usuario validando propiedad.
     *
     * @param usuarioIdRuta  ID del usuario en la ruta.
     * @param usuarioIdToken ID del usuario autenticado vía token.
     * @return RespuestaPerfilFinanciero con el perfil consultado.
     */
    RespuestaPerfilFinanciero consultar(UUID usuarioIdRuta, UUID usuarioIdToken);
}

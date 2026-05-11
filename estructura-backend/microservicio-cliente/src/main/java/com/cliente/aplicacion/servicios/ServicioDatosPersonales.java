package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.SolicitudDatosPersonales;
import com.cliente.dominio.entidades.DatosPersonales;

import java.util.UUID;

/**
 * Interfaz de servicio para la gestion de datos personales del cliente.
 * <p>
 * Se encarga la agrupación de los datos personales del cliente
 * </p>
 *
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioDatosPersonales {

    /**
     * Crea un registro vacío de datos personales vinculado al usuarioId.
     * Idempotente: si ya existe, lo devuelve sin crear uno nuevo.
     *
     * @param usuarioId ID del usuario
     * @return RespuestaDatosPersonales DTO de respuesta.
     */
    RespuestaDatosPersonales crearPerfil(UUID usuarioId);

    /**
     * Actualiza los datos personales del cliente con validación de propiedad.
     *
     * @param usuarioIdRuta ID del usuario en la ruta
     * @param usuarioIdToken ID del usuario autenticado
     * @param solicitud DTO con datos a actualizar
     * @param ipOrigen IP del cliente
     * @return RespuestaDatosPersonales DTO de respuesta.
     */
    public RespuestaDatosPersonales actualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudDatosPersonales solicitud, String ipOrigen);

    /**
     * Consulta los datos personales de un usuario, validando propiedad.
     *
     * @param usuarioIdRuta ID del usuario en la ruta
     * @param usuarioIdToken ID del usuario autenticado
     * @return RespuestaDatosPersonales DTO de respuesta.
     */
    public RespuestaDatosPersonales consultar(UUID usuarioIdRuta, UUID usuarioIdToken);

    /**
     * Convierte una entidad al DTO de respuesta.
     *
     * @param datos Información del cliente
     * @return RespuestaDatosPersonales DTO de respuesta.
     */
    public RespuestaDatosPersonales convertirADTO(DatosPersonales datos);
}

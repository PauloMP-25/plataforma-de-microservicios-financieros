package com.cliente.aplicacion.puertos;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudDatosPersonales;

import java.util.UUID;

/**
 * Interfaz de puerto para la gestión de datos personales del cliente.
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
     */
    RespuestaDatosPersonales actualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudDatosPersonales solicitud, String ipOrigen);

    /**
     * Consulta los datos personales de un usuario, validando propiedad.
     */
    RespuestaDatosPersonales consultar(UUID usuarioIdRuta, UUID usuarioIdToken);

    /**
     * Actualiza solo el teléfono del usuario (uso interno para sincronización OTP).
     */
    void actualizarTelefono(UUID usuarioId, String telefono);

    /**
     * Consulta interna de datos personales sin validación de JWT (uso para Facade).
     */
    RespuestaDatosPersonales consultarInterno(UUID usuarioId);
}

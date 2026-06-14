package com.usuario.aplicacion.puertos;

import java.util.UUID;

/**
 * Puerto de aplicación para el servicio de caché de perfiles externos.
 * Define la capacidad de consultar y cachear información técnica como números de teléfono.
 */
public interface IServicioPerfilCache {

    /**
     * Obtiene el teléfono del usuario desde el microservicio cliente y lo almacena en caché.
     *
     * @param usuarioId ID del usuario a consultar.
     * @return El número de teléfono recuperado.
     */
    String obtenerTelefono(UUID usuarioId);
}

package com.mensajeria.aplicacion.dtos;

/**
 * Respuesta estándar tras validar un código.
 */
public record RespuestaValidacion(
        boolean exito,
        String mensaje) {
}

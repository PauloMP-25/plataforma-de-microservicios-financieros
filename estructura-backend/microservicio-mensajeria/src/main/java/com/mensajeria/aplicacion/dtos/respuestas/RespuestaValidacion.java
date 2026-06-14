package com.mensajeria.aplicacion.dtos.respuestas;

/**
 * Respuesta estándar tras validar un código.
 */
public record RespuestaValidacion(
        boolean exito,
        String mensaje) {
}

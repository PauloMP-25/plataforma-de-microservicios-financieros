package com.usuario.aplicacion.dtos.respuestas;

public record RespuestaValidacion(
        boolean esValido,
        String mensajeValidacion
) {
}

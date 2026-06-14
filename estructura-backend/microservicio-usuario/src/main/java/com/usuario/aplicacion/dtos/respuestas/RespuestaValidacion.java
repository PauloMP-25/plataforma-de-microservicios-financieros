package com.usuario.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RespuestaValidacion(
        @JsonAlias({"exito", "esValido"})
        boolean esValido,
        @JsonAlias({"mensaje", "mensajeValidacion"})
        String mensajeValidacion
) {
}

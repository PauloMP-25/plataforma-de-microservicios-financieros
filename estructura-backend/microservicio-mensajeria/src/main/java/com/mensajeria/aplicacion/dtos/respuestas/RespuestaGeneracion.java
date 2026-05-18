package com.mensajeria.aplicacion.dtos.respuestas;

public record RespuestaGeneracion(
    boolean exito,
    String mensaje,
    String tipo
) {}

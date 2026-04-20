package com.mensajeria.aplicacion.dtos;

public record RespuestaGeneracion(
    boolean exito,
    String mensaje,
    String tipo
) {}

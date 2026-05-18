package com.mensajeria.aplicacion.dtos.respuestas;

import com.libreria.comun.enums.TipoVerificacion;

public record RespuestaGeneracion(
    boolean exito,
    String mensaje,
    TipoVerificacion tipo
) {}

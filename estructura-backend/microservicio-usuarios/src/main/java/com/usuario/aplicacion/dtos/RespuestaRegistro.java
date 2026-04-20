package com.usuario.aplicacion.dtos;

import lombok.Builder;

@Builder
public record RespuestaRegistro(
        String idUsuario,
        String nombreUsuario,
        String correo,
        String mensaje) {

}

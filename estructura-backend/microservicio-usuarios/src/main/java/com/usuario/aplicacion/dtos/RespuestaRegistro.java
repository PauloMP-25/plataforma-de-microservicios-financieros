package com.usuario.aplicacion.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RespuestaRegistro {
    private String mensaje;
    private String nombreUsuario;
    private String correo;
    private String tokenConfirmacion;
}

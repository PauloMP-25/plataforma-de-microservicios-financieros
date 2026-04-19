package com.usuario.aplicacion.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RespuestaRegistro {
    private String idUsuario;
    private String nombreUsuario;
    private String correo;
    private String tokenConfirmacion;
    
}

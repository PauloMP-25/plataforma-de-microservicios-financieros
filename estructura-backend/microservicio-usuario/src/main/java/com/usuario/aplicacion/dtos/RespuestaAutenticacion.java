package com.usuario.aplicacion.dtos;

import java.util.List;

public record RespuestaAutenticacion(
        String tokenAcceso,
        String tipoToken,
        long expiraEn,
        String idUsuario,
        String nombreUsuario,
        List<String> roles) {

    public static RespuestaAutenticacion of(String token, long expiraEn, String idUsuario, String nombreUsuario, List<String> roles) {
        return new RespuestaAutenticacion(token, "Bearer", expiraEn, idUsuario, nombreUsuario, roles);
    }
}



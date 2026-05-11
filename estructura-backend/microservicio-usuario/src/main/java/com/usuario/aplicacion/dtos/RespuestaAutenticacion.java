package com.usuario.aplicacion.dtos;

import java.util.List;

public record RespuestaAutenticacion(
        String tokenAcceso,
        String refreshToken,
        String tipoToken,
        long expiraEn,
        long refreshExpiraEn,
        String idUsuario,
        String nombreUsuario,
        List<String> roles) {

    public static RespuestaAutenticacion of(String token, String refreshToken, long expiraEn, long refreshExpiraEn, 
                                            String idUsuario, String nombreUsuario, List<String> roles) {
        return new RespuestaAutenticacion(token, refreshToken, "Bearer", expiraEn, refreshExpiraEn, idUsuario, nombreUsuario, roles);
    }
}

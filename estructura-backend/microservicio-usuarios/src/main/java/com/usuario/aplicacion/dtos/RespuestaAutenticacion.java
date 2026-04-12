package com.usuario.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Respuesta exitosa de autenticación. Contiene el JWT y metadatos del token.
 * @author Paulo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaAutenticacion(
        String tokenAcceso,
        String tipoToken,
        long expiraEn,
        String nombreUsuario,
        List<String> roles
) {
    public static RespuestaAutenticacion of(String token, long expiraEn, String nombreUsuario, List<String> roles) {
        return new RespuestaAutenticacion(token, "Bearer", expiraEn, nombreUsuario, roles);
    }
}

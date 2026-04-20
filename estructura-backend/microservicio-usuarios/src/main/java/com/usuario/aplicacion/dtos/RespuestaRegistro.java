package com.usuario.aplicacion.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RespuestaRegistro {
    private String idUsuario;      // Útil para futuras referencias en el flujo
    private String nombreUsuario;
    private String correo;         // Para mostrar en la vista de "Confirmar código"
    private String mensaje;        // "Registro exitoso. Revise su correo."
}

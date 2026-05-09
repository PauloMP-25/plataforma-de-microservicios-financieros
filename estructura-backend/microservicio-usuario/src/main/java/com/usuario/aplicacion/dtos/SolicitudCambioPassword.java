package com.usuario.aplicacion.dtos;

import com.usuario.dominio.validaciones.ValidarPassword;
import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author user
 */
// En com.usuario.aplicacion.dtos
public record SolicitudCambioPassword(
        @NotBlank(message = "La contraseña es obligatoria")
        String passwordActual,
        
        @NotBlank(message = "La contraseña es obligatoria")
        
        @ValidarPassword
        String nuevoPassword,
        
        @NotBlank
        String confirmarPassword) {

    public boolean contrasenasNuevasCoinciden() {
        return nuevoPassword.equals(confirmarPassword);
    }
}

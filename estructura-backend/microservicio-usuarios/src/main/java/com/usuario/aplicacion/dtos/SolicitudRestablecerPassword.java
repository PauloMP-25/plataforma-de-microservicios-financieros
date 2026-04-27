package com.usuario.aplicacion.dtos;

import com.usuario.dominio.validaciones.ValidarPassword;
import jakarta.validation.constraints.NotBlank;

public record SolicitudRestablecerPassword(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @ValidarPassword
        String nuevoPassword,
        @NotBlank(message = "Debe confirmar la contraseña")
        String confirmarPassword) {

    public boolean contrasenasNuevasCoinciden() {
        return nuevoPassword.equals(confirmarPassword);
    }
}

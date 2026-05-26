package com.usuario.aplicacion.dtos.solicitudes;

import com.usuario.dominio.validaciones.ValidarPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para restablecer la contraseña mediante OTP de recuperación.
 * El usuario envía su correo, el código OTP recibido y la nueva contraseña.
 */
public record SolicitudRestablecerPassword(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        String correo,
        @NotBlank(message = "El código OTP es obligatorio")
        String codigoOtp,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @ValidarPassword
        String nuevoPassword,
        @NotBlank(message = "Debe confirmar la contraseña")
        String confirmarPassword) {

    public boolean contrasenasNuevasCoinciden() {
        return nuevoPassword.equals(confirmarPassword);
    }
}

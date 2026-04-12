package com.usuario.aplicacion.dtos;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload de inicio de sesión. Recibe usuario y contraseña en texto plano.
 * @author Paulo
 */
public class SolicitudLogin {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    String nombreUsuario,

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una letra minuscula")
    @Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una letra mayuscula")
    @Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
    @Pattern(regexp = ".{6,}", message = "Debe tener al menos 6 caracteres")
    String password
}

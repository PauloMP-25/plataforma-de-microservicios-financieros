package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudLogin(
        @NotBlank(message = "El correo es obligatorio")
        @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        @Email(message = "El correo debe tener un formato válido")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una letra minuscula")
        @Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una letra mayuscula")
        @Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
        @Pattern(regexp = ".*[@$!%*?&#].*", message = "Debe contener al menos un carácter especial (@$!%*?&)")
        @Pattern(regexp = ".{8,}", message = "Debe tener al menos 8 caracteres")
        String password) {

}

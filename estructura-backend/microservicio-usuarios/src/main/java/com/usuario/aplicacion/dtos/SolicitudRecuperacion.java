package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudRecuperacion(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String correo) {

}

package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudRecuperacion(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String correo,
        @NotBlank(message = "El teléfono es obligatorio")
        String telefono,
        
        @NotNull(message = "El tipo de canal es obligatorio")
        TipoVerificacion tipo // <--- Faltaba este campo
) {
}

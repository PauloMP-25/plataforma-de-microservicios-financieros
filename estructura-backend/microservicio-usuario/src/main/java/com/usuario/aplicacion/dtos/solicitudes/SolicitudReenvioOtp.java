package com.usuario.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import com.libreria.comun.enums.TipoVerificacion;

public record SolicitudReenvioOtp(
        @NotNull(message = "El email es obligatorio")
        @Email(message = "Debe ser un correo electrónico válido")
        String email,

        String telefono,

        @NotNull(message = "El tipo de canal es obligatorio")
        TipoVerificacion tipo
) {
}

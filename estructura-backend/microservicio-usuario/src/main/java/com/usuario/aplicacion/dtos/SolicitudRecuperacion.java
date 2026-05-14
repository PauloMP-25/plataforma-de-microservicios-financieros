package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.NotNull;

public record SolicitudRecuperacion(
        String correo,

        String telefono,

        @NotNull(message = "El tipo de canal es obligatorio")
                com.libreria.comun.enums.TipoVerificacion tipo) {
}

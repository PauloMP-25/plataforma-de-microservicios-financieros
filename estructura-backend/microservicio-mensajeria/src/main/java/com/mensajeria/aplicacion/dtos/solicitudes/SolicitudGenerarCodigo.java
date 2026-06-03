package com.mensajeria.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record SolicitudGenerarCodigo(
                @NotNull(message = "El usuarioId es obligatorio") UUID usuarioId,

                @Email(message = "Formato de email inválido")
                String email,

                @Pattern(regexp = "^\\+[1-9]\\d{9,14}$", message = "El teléfono debe tener formato E.164: +51XXXXXXXXX")
                String telefono,

                @NotNull(message = "El tipo de canal es obligatorio") com.libreria.comun.enums.TipoVerificacion tipo,

                @NotNull(message = "El propósito del código es obligatorio") com.libreria.comun.enums.PropositoCodigo proposito) {

    @AssertTrue(message = "Falta el dato de contacto requerido para el canal seleccionado")
    public boolean isContactoValido() {
        if (tipo == null) return true; // Let the @NotNull annotation handle this
        
        if (tipo == com.libreria.comun.enums.TipoVerificacion.EMAIL) {
            return email != null && !email.isBlank();
        } else {
            return telefono != null && !telefono.isBlank();
        }
    }
}

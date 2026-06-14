package com.usuario.aplicacion.dtos.solicitudes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Modelo de solicitud para la autenticación de usuarios.")
public record SolicitudLogin(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        @Schema(description = "Correo electrónico registrado del usuario.", example = "paulo@luka-financial.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo,
        
        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(description = "Contraseña de la cuenta del usuario.", example = "adminUTP123$", requiredMode = Schema.RequiredMode.REQUIRED)
        String password) {

}

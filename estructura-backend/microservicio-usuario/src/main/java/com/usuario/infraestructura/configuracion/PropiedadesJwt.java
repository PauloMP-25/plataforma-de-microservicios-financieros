package com.usuario.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas para la gestión de tokens JWT.
 * Falla rápido en el arranque si las variables de entorno no están configuradas correctamente.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "luka.jwt")
public class PropiedadesJwt {

    @NotBlank(message = "La clave secreta de JWT (luka.jwt.clave-secreta) es obligatoria y no puede estar vacía.")
    private String claveSecreta;

    @NotNull(message = "El tiempo de expiración del token JWT (luka.jwt.expiracion-ms) es obligatorio.")
    private Long expiracionMs;
}

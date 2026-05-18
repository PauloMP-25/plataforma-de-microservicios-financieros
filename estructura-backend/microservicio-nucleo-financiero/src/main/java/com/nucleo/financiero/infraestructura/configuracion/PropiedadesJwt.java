package com.nucleo.financiero.infraestructura.configuracion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Propiedades de configuración tipadas y validadas para la seguridad JWT.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "luka.jwt")
public class PropiedadesJwt {

    @NotBlank(message = "La clave secreta JWT ('luka.jwt.clave-secreta') no puede estar vacía en la configuración.")
    private String claveSecreta;

    @NotNull(message = "El tiempo de expiración de JWT ('luka.jwt.expiracion-ms') debe estar especificado.")
    private Long expiracionMs;
}

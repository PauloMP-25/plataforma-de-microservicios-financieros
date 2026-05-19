package com.mensajeria.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de configuración para seguridad JWT.
 */
@Configuration
@ConfigurationProperties(prefix = "luka.jwt")
@Validated
@Data
public class PropiedadesJwt {

    @NotBlank(message = "La clave secreta JWT es obligatoria")
    private String claveSecreta;

}

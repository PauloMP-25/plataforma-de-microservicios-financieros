package com.nucleo.financiero.infraestructura.configuracion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

/**
 * Propiedades de configuración tipadas y validadas para los endpoints de integración inter-microservicios.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "microservicio.ia")
public class PropiedadesInfraestructura {

    @NotBlank(message = "La URL de integración con el microservicio de IA ('microservicio.ia.url') no puede estar vacía.")
    private String url;
}

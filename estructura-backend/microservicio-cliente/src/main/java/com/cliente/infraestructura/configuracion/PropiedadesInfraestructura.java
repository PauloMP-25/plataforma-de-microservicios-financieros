package com.cliente.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas de integración inter-servicio para microservicio-cliente.
 * Almacena las URLs de integración de clientes Feign, previniendo errores de arranque.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "microservicio")
public class PropiedadesInfraestructura {

    private final NucleoFinanciero nucleoFinanciero = new NucleoFinanciero();

    @Getter
    @Setter
    public static class NucleoFinanciero {
        @NotBlank(message = "La URL del microservicio de núcleo financiero (microservicio.nucleo.financiero.url) es obligatoria y no puede estar vacía.")
        private String url;
    }
}

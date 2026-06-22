package com.mensajeria.infraestructura.configuracion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapeo centralizado de las propiedades personalizadas de Email.
 */
@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class PropiedadesEmail {

    private String from;

    /**
     * Dirección de destino para los correos de soporte al cliente.
     * Configurable via {@code email.soporte} en application.yml o variable de entorno {@code EMAIL_SOPORTE}.
     */
    private String soporte;

    private Nombre nombre = new Nombre();

    @Data
    public static class Nombre {
        private String empresa = "Luka App";
    }
}

package com.usuario.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas de infraestructura externa para microservicio-usuario.
 * Almacena las URLs de integración de clientes Feign, previniendo errores de arranque.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "microservicio")
public class PropiedadesInfraestructura {

    private final Cliente cliente = new Cliente();
    private final Mensajeria mensajeria = new Mensajeria();

    @Getter
    @Setter
    public static class Cliente {
        @NotBlank(message = "La URL del microservicio de cliente (microservicio.cliente.url) es obligatoria y no puede estar vacía.")
        private String url;
    }

    @Getter
    @Setter
    public static class Mensajeria {
        @NotBlank(message = "La URL del microservicio de mensajería (microservicio.mensajeria.url) es obligatoria y no puede estar vacía.")
        private String url;
    }
}

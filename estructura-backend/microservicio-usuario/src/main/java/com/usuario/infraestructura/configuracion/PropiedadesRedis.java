package com.usuario.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas para el servidor Redis de caché y listas negras de tokens.
 * Proporciona valores por defecto seguros para facilitar el desarrollo local sin configuraciones redundantes.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "spring.data.redis")
public class PropiedadesRedis {

    @NotBlank(message = "El host de Redis (spring.data.redis.host) es obligatorio.")
    private String host = "localhost";

    @NotNull(message = "El puerto de Redis (spring.data.redis.port) es obligatorio.")
    private Integer port = 6379;

    private String password;
}

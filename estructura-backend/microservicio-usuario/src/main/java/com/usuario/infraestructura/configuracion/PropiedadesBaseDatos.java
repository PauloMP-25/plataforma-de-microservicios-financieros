package com.usuario.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas para la conexión a la base de datos relacional.
 * Evita configuraciones incompletas y fallos tardíos de conexión SQL.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "spring.datasource")
public class PropiedadesBaseDatos {

    @NotBlank(message = "La URL de la base de datos (spring.datasource.url) es obligatoria y no puede estar en blanco.")
    private String url;

    @NotBlank(message = "El usuario de la base de datos (spring.datasource.username) es obligatorio.")
    private String username;

    @NotBlank(message = "La contraseña de la base de datos (spring.datasource.password) es obligatoria.")
    private String password;

    @NotBlank(message = "El driver de la base de datos (spring.datasource.driver-class-name) es obligatorio.")
    private String driverClassName;
}

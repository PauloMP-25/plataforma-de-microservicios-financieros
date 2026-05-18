package com.mensajeria.infraestructura.configuracion;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de configuración para la lógica de OTP.
 */
@Configuration
@ConfigurationProperties(prefix = "mensajeria.otp")
@Validated
@Data
public class PropiedadesOtp {

    @Positive(message = "El tiempo de expiración debe ser positivo")
    private int expiracionMinutos = 10;

    @Positive(message = "El máximo de intentos debe ser positivo")
    private int maxIntentos = 3;

    @Positive(message = "Las horas de bloqueo deben ser positivas")
    private long bloqueoHoras = 10;
}

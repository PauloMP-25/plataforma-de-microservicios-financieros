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

    /**
     * Tiempo de vida del código OTP antes de expirar.
     * Rango recomendado: 5 a 15 minutos.
     * Por defecto: 10 minutos.
     */
    @Positive(message = "El tiempo de expiración debe ser positivo")
    private int expiracionMinutos = 10;

    /**
     * Número máximo de intentos fallidos antes de bloquear la cuenta.
     * Rango recomendado: 3 a 5 intentos.
     * Por defecto: 3 intentos.
     */
    @Positive(message = "El máximo de intentos debe ser positivo")
    private int maxIntentos = 3;

    /**
     * Tiempo de castigo para una cuenta bloqueada por exceder los intentos.
     * Rango recomendado: 1 a 24 horas.
     * Por defecto: 10 horas.
     */
    @Positive(message = "Las horas de bloqueo deben ser positivas")
    private long bloqueoHoras = 10;
}

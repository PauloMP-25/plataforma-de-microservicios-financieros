package com.mensajeria.infraestructura.salud;

import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado para la integración de Twilio.
 * Si Twilio falla, reporta un estado detallado sin comprometer
 * la salud global del microservicio (evitando ciclos de reinicio en Render).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TwilioHealthIndicator implements HealthIndicator {

    private final IMensajeriaService mensajeriaService;

    @Override
    public Health health() {
        try {
            mensajeriaService.validarConexionTwilio();
            return Health.up()
                    .withDetail("proveedor", "Twilio")
                    .withDetail("conexion", "exitosa")
                    .build();
        } catch (Exception e) {
            log.warn("[HEALTH-CHECK] Twilio reportó problemas de conexión o credenciales: {}", e.getMessage());
            // Si falla la validación, reportamos UNKNOWN con los detalles del error.
            // Esto evita que Actuator ponga el estado global en DOWN, previniendo
            // reinicios indeseados en Render, pero permitiendo auditar el fallo.
            return Health.unknown()
                    .withDetail("proveedor", "Twilio")
                    .withDetail("conexion", "fallida")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

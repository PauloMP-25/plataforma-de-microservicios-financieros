package com.mensajeria.infraestructura.salud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Indicador de salud personalizado para la integración de Gmail SMTP.
 * Verifica si se puede conectar al servidor SMTP configurado.
 * Retorna UNKNOWN en caso de fallo para no alterar el estado de salud global
 * del microservicio (evitando reinicios innecesarios en Render/Docker),
 * pero informando adecuadamente el estado del servicio de correo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GmailHealthIndicator implements HealthIndicator {

    private final JavaMailSender mailSender;

    @Override
    public Health health() {
        try {
            if (mailSender instanceof JavaMailSenderImpl javaMailSenderImpl) {
                javaMailSenderImpl.testConnection();
            }
            return Health.up()
                    .withDetail("proveedor", "Gmail SMTP")
                    .withDetail("conexion", "Servicio de correo operativo")
                    .build();
        } catch (Exception e) {
            log.warn("[HEALTH-CHECK] Gmail SMTP reportó problemas de conexión o credenciales: {}", e.getMessage());
            return Health.unknown()
                    .withDetail("proveedor", "Gmail SMTP")
                    .withDetail("conexion", "Servicio de correo con problemas")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

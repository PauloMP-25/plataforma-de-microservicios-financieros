package com.pagos.infraestructura.configuracion;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de pagos.
 * Aplica el patrón de seguridad de la plataforma LUKA APP:
 * 1. Los webhooks de Stripe son PÚBLICOS (se validan internamente mediante firma HMAC).
 * 2. Los endpoints de checkout y suscripción requieren un JWT válido.
 * 3. Los endpoints administrativos requieren ROLE_ADMIN.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SEGURIDAD] Configurando filtros para microservicio-pago...");

        // 1. Cargamos la configuración base (CORS, CSRF, JWT Filter, Stateless)
        super.configurarAutorizacion(http);

        // 2. Reglas de autorización específicas
        http.authorizeHttpRequests(auth -> auth
                // WEBHOOK: Debe ser público para que Stripe pueda enviar notificaciones
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/webhook/**").permitAll()

                // INFRAESTRUCTURA: Monitoreo y Documentación
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                .anyRequest().authenticated()
        );

        return http.build();
    }
}

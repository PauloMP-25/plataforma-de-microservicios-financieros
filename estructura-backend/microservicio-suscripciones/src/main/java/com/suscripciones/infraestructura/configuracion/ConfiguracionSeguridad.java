package com.suscripciones.infraestructura.configuracion;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de suscripciones.
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la lógica de JWT
 * y el filtro CORS stateless de la plataforma LUKA APP.
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
        log.info("[SEGURIDAD] Configurando filtros para microservicio-suscripciones...");

        // 1. Cargamos la configuración base (CORS, CSRF, JWT Filter, Stateless)
        super.configurarAutorizacion(http);

        // 2. Reglas de autorización específicas
        http.authorizeHttpRequests(auth -> auth
                // INFRAESTRUCTURA: Monitoreo y Documentación
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Endpoints de negocio protegidos
                .requestMatchers("/api/v1/suscripciones/**").hasAnyRole("FREE", "PREMIUM", "PRO", "ADMIN", "ADMINISTRADOR")

                // Resto de peticiones autenticadas
                .anyRequest().authenticated()
        );

        return http.build();
    }
}

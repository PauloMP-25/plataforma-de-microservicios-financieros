package com.pagos.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de pagos.
 * Extiende de ConfiguracionSeguridadBase para heredar la configuración de la plataforma
 * (CORS, JWT, Stateless).
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. Configuramos la base (JWT, Stateless, Exception handling, CORS)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio
        http.authorizeHttpRequests(auth -> auth
                // Endpoints de pagos (Requieren autenticación)
                .requestMatchers("/api/v1/pagos/**").authenticated()
                
                // Monitoreo y Documentación (Público)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                .anyRequest().authenticated());

        return http.build();
    }
}

package com.financiero.saas.eureka.Configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad del Eureka Server.
 *
 * Por defecto Spring Security bloquea el dashboard con CSRF habilitado,
 * lo que impide que los microservicios clientes se registren (usan POST).
 * Esta clase deshabilita CSRF para las rutas de Eureka y exige
 * autenticación HTTP Basic para acceder al dashboard.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridadEureka {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. Configuración de infraestructura Eureka
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"));

        // 2. Reglas de acceso (Monitoreo y Dashboard)
        http.authorizeHttpRequests(auth -> auth
                // Monitoreo y Documentación (Público)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Dashboard y registro requieren autenticación
                .anyRequest().authenticated()
        );

        // 3. Mecanismos de Autenticación
        http.formLogin(Customizer.withDefaults()) // Dashboard web
                .httpBasic(Customizer.withDefaults()); // Clientes Eureka

        return http.build();
    }
}

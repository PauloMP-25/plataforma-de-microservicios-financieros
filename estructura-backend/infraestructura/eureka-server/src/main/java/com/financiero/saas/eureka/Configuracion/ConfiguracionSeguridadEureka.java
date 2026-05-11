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
        http
            // Sin esto, los microservicios clientes NO pueden registrarse
            // porque sus peticiones POST de heartbeat fallan con 403.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**")
            )
            .authorizeHttpRequests(auth -> auth
                // El actuator de health es público para que el Gateway y otros sistemas puedan verificar el estado.
                // --- Monitoreo y Documentación (Público) ---
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Activa el formulario de login del dashboard web
            .formLogin(Customizer.withDefaults())
            // Activa HTTP Basic para que los clientes Eureka puedan
            // autenticarse en la URL de registro
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

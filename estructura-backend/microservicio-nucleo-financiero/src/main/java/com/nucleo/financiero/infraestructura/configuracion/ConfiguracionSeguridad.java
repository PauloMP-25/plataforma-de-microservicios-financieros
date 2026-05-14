package com.nucleo.financiero.infraestructura.configuracion;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Seguridad para el Núcleo Financiero.
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la lógica de
 * autenticación JWT.
 * Define reglas de autorización específicas para este microservicio.
 *
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Define la cadena de filtros de seguridad específica para el Núcleo
     * Financiero.
     * 
     * @param http Configuración de seguridad
     * @return SecurityFilterChain configurado
     * @throws Exception Si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        // Deshabilitar CSRF (estándar para APIs Stateless)
        http.csrf(AbstractHttpConfigurer::disable);

        // Aplicar configuración base (JWT, Actuator, Swagger)
        configurarAutorizacion(http);

        // Añadir reglas de autorización específicas del dominio financiero
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/financiero/categorias/**").hasAnyRole("USUARIO", "ADMIN")
                .requestMatchers("/api/v1/financiero/transacciones/**").hasAnyRole("USUARIO", "ADMIN")
                // --- Monitoreo y Documentación (Público) ---
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                .permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}

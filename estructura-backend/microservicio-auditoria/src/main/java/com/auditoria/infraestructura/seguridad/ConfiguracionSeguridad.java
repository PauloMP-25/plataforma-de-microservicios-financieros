package com.auditoria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad específica para el microservicio de auditoría.
 * <p>
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la gestión de JWT
 * y define los permisos de acceso a los endpoints de auditoría.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.2.0
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Define la cadena de filtros de seguridad.
     * Inyecta la configuración base y añade reglas específicas del negocio.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return configurarAutorizacion(http).build();
    }

    @Override
    protected HttpSecurity configurarAutorizacion(HttpSecurity http) throws Exception {
        // Desactivamos CSRF ya que usamos JWT (Stateless)
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Endpoints públicos o de infraestructura
                .requestMatchers("/api/v1/auditoria/seguridad/verificar-ip/**").permitAll()

                // Las consultas detalladas de auditoría solo para administradores
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/accesos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/registros/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/transacciones/**").hasRole("ADMIN")
                // --- Monitoreo y Documentación (Público) ---
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                .permitAll()

                // El resto de rutas heredadas (Swagger, Actuator) y autenticación general
                .anyRequest().authenticated());

        return super.configurarAutorizacion(http);
    }
}

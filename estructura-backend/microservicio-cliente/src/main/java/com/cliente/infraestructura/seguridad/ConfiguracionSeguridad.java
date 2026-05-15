package com.cliente.infraestructura.seguridad;

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
 * Configuración de seguridad específica para el microservicio de cliente.
 * <p>
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la gestión de JWT
 * y define los permisos de acceso a los endpoints de cliente.
 * </p>
 * 
 * @author Paulo Moron
 * @since 2026-09
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    /**
     * Constructor que inyecta las dependencias de seguridad de la librería común.
     *
     * @param filtroJwt       Filtro JWT para validación de tokens.
     * @param puntoEntradaJwt Punto de entrada para respuestas de error 401.
     */
    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Configura la cadena de filtros de seguridad específica para el microservicio
     * de cliente.
     * 
     * @param http Configuración de seguridad de Spring.
     * @return {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Configuramos la base (JWT, Stateless, Exception handling)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio (De lo más específico a lo
        // general)
        http.authorizeHttpRequests(auth -> auth
                // Endpoints internos (comunicación inter-microservicio)
                .requestMatchers("/api/v1/clientes/interno/**").permitAll()

                // Perfil de datos personales
                .requestMatchers(HttpMethod.PUT, "/api/v1/clientes/perfil/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/clientes/perfil/**").authenticated()

                // Perfil financiero
                .requestMatchers("/api/v1/clientes/perfil-financiero/**").authenticated()

                // Metas de ahorro
                .requestMatchers("/api/v1/clientes/metas/**").authenticated()

                // Límites de gasto
                .requestMatchers("/api/v1/clientes/limites/**").authenticated()

                // Monitoreo y Documentación (Público)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 3. BLOQUEO TOTAL AL FINAL
                .anyRequest().authenticated());

        return http.build();
    }
}

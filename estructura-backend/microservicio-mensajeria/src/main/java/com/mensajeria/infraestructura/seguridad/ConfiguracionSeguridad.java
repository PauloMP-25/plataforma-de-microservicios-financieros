package com.mensajeria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security para el microservicio de mensajería.
 * <p>
 * Hereda de {@link ConfiguracionSeguridadBase} la configuración stateless
 * común (deshabilitar CSRF, sesión sin estado, punto de entrada JWT y filtros
 * de infraestructura) y añade únicamente las rutas públicas propias de este
 * microservicio (endpoints OTP).
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    /**
     * Construye la configuración inyectando el filtro JWT y el punto de entrada
     * de la librería común mediante inyección por constructor.
     *
     * @param filtroJwt       Filtro centralizado que valida el token JWT en cada
     *                        petición autenticada.
     * @param puntoEntradaJwt Manejador que devuelve HTTP 401 en JSON cuando no
     *                        hay token o es inválido.
     */
    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Define la cadena de filtros de seguridad del microservicio.
     * <p>
     * Llama a {@code configurarAutorizacion} de la clase base para aplicar la
     * política stateless y luego permite de forma explícita los endpoints OTP,
     * que son públicos por diseño (el usuario aún no está autenticado cuando
     * solicita o valida su OTP).
     * </p>
     *
     * @param http Objeto de configuración de Spring Security.
     * @return {@link SecurityFilterChain} con las reglas de este microservicio.
     * @throws Exception si la configuración de Spring Security falla.
     */
    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http) throws Exception {
        super.configurarAutorizacion(http);

        // 2. Añadimos las reglas específicas del microservicio
        http.authorizeHttpRequests(auth -> auth
                // Rutas públicas de este microservicio
                .requestMatchers("/api/v1/clientes/interno/**").permitAll()

                // --- Monitoreo y Documentación ---
                // Nota: Tu librería ya tiene esto, pero ponerlo aquí explícitamente no falla
                // siempre y cuando se haga ANTES del anyRequest.
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Rutas protegidas
                .requestMatchers(HttpMethod.GET, "/api/v1/clientes/perfil/**").authenticated()
                .requestMatchers("/api/v1/clientes/metas/**").authenticated()

                // 3. EL CIERRE TOTAL SIEMPRE AL FINAL
                .anyRequest().authenticated()
        );

        return http.build();
    }
}

package com.libreria.comun.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración base de seguridad para el ecosistema LUKA APP.
 * <p>
 * Proporciona los cimientos de una arquitectura Stateless:
 * 1. Deshabilita CSRF y sesiones (Stateless).
 * 2. Configura el Punto de Entrada para errores 401 en JSON.
 * 3. Inyecta el Filtro JWT centralizado.
 * 4. Define rutas de infraestructura comunes (Swagger, Actuator).
 * </p>
 *
 * @author Paulo Moron
 */
@RequiredArgsConstructor
public abstract class ConfiguracionSeguridadBase {

    protected final FiltroJwt filtroJwt;
    protected final PuntoEntradaJwt puntoEntradaJwt;

    /**
     * Define la configuración común de la cadena de filtros.
     * Los microservicios deben llamar a este método y añadir sus rutas específicas.
     *
     * @param http Configuración de seguridad de Spring.
     * @return {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    protected HttpSecurity configurarBase(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(puntoEntradaJwt))
                .authorizeHttpRequests(auth -> auth
                        // Rutas transversales de infraestructura
                        .requestMatchers("/actuator/**", "/error/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                )
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * Bean de codificación de contraseñas único para toda la plataforma.
     * Se usa BCrypt con fuerza 12 para máxima seguridad.
     * @return password
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

package com.libreria.comun.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuración base de seguridad para el ecosistema LUKA APP.
 * <p>
 * Proporciona los cimientos de una arquitectura Stateless: 1. Deshabilita CSRF
 * y sesiones (Stateless). 2. Configura el Punto de Entrada para errores 401 en
 * JSON. 3. Inyecta el Filtro JWT centralizado. 4. Define rutas de
 * infraestructura comunes (Swagger, Actuator). 5. Habilita CORS para permitir
 * peticiones desde el frontend.
 * </p>
 *
 * @author Paulo Moron
 */
@RequiredArgsConstructor
public abstract class ConfiguracionSeguridadBase {

    protected final FiltroJwt filtroJwt;
    protected final PuntoEntradaJwt puntoEntradaJwt;

    @org.springframework.beans.factory.annotation.Autowired
    protected FiltroAutenticacionInterna filtroAutenticacionInterna;

    /**
     * Orígenes CORS permitidos en producción, separados por coma.
     * Si no se define, el método {@code corsConfigurationSource()} usa el
     * fallback de localhost para desarrollo local.
     */
    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsAllowedOriginsEnv;

    /**
     * Define la configuración común de la cadena de filtros. Los microservicios
     * deben llamar a este método y añadir sus rutas específicas.
     *
     * @param http Configuración de seguridad de Spring.
     * @return {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    protected HttpSecurity configurarAutorizacion(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(puntoEntradaJwt))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; font-src 'self' data: https://fonts.gstatic.com https://cdnjs.cloudflare.com; img-src 'self' data: blob:; connect-src 'self' *;"))
                )
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroAutenticacionInterna, FiltroJwt.class);
    }

    /**
     * Configuración de CORS dinámica.
     * <p>
     * Lee la variable de entorno {@code CORS_ALLOWED_ORIGINS} (lista separada
     * por comas). Si no está definida o está vacía, usa el conjunto de
     * orígenes localhost como fallback para el entorno de desarrollo local.
     * </p>
     *
     * @return {@link CorsConfigurationSource} configurado.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origenesLocalhostFallback = List.of(
                "http://localhost:8081",
                "http://localhost:8082",
                "http://localhost:8083",
                "http://localhost:8084",
                "http://localhost:8085",
                "http://localhost:8086",
                "http://localhost:61274",
                "http://localhost:4200",
                "http://localhost:4201",
                "http://localhost:61878",
                "http://localhost:5173"
        );

        List<String> origenes;
        if (corsAllowedOriginsEnv != null && !corsAllowedOriginsEnv.isBlank()) {
            origenes = Arrays.stream(corsAllowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            origenes = origenesLocalhostFallback;
        }

        configuration.setAllowedOrigins(origenes);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bean de codificación de contraseñas único para toda la plataforma. Se usa
     * BCrypt con fuerza 12 para máxima seguridad.
     *
     * @return password
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

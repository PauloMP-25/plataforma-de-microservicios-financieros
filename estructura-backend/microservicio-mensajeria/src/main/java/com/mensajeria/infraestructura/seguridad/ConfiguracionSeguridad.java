package com.mensajeria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * @param filtroJwt        Filtro centralizado que valida el token JWT en cada
     *                         petición autenticada.
     * @param puntoEntradaJwt  Manejador que devuelve HTTP 401 en JSON cuando no
     *                         hay token o es inválido.
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
        return configurarAutorizacion(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/mensajeria/otp/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}

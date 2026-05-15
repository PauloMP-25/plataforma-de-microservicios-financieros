package com.nucleo.financiero.infraestructura.configuracion;

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
     * Configura la cadena de filtros de seguridad específica para el microservicio
     * de núcleo financiero.
     * 
     * @param http Configuración de seguridad
     * @return SecurityFilterChain configurado
     * @throws Exception Si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Configuramos la base (JWT, Stateless, Exception handling)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio (De lo más específico a lo
        // general)
        http.authorizeHttpRequests(auth -> auth
                // Endpoints de negocio financiero
                .requestMatchers("/api/v1/financiero/categorias/**").hasAnyRole("USUARIO", "ADMIN")
                .requestMatchers("/api/v1/financiero/transacciones/**").hasAnyRole("USUARIO", "ADMIN")

                // Monitoreo y Documentación (Público)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 3. BLOQUEO TOTAL AL FINAL
                .anyRequest().authenticated());

        return http.build();
    }
}

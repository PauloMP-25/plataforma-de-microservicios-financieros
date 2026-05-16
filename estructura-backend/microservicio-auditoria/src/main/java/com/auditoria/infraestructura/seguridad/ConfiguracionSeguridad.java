package com.auditoria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    /**
     * Configura la cadena de filtros de seguridad específica para el microservicio
     * de auditoría.
     * 
     * @param http Configuración HttpSecurity.
     * @return SecurityFilterChain configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Configuramos la base (JWT, Stateless, Exception handling)
        super.configurarAutorizacion(http);

        // 2. Definimos las reglas de este microservicio (De lo más específico a lo
        // general)
        http.authorizeHttpRequests(auth -> auth
                // Endpoints públicos o de infraestructura
                .requestMatchers("/api/v1/auditoria/seguridad/verificar-ip/**").permitAll()

                // Las consultas detalladas de auditoría solo para administradores
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/accesos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/registros/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/auditoria/transacciones/**").hasRole("ADMIN")

                // Monitoreo y Documentación (Público)
                .requestMatchers("/actuator/**", "/error/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 3. BLOQUEO TOTAL AL FINAL
                .anyRequest().authenticated());

        return http.build();
    }
}

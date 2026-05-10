package com.auditoria.infraestructura.seguridad;

import com.financiero.saas.comun.seguridad.ConfiguracionSeguridadBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Configuración de seguridad específica para el microservicio de auditoría.
 * <p>
 * Extiende de {@link ConfiguracionSeguridadBase} para heredar la gestión de JWT
 * y define los permisos de acceso a los endpoints de auditoría.
 * </p>
 * 
 * @author Paulo Moron
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    @Override
    protected void configurarAutorizacion(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // El endpoint de verificación de IP para el Gateway debe ser público o interno
            .requestMatchers("/api/v1/auditoria/seguridad/verificar-ip/**").permitAll()
            
            // Las consultas detalladas solo para administradores
            .requestMatchers("/api/v1/auditoria/registros/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/auditoria/accesos/**").hasRole("ADMIN")
            
            // El resto requiere autenticación
            .anyRequest().authenticated()
        );
    }
}

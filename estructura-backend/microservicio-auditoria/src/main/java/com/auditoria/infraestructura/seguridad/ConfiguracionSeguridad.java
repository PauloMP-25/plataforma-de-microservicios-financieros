package com.auditoria.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
        // TODO Auto-generated constructor stub
    }

    /**
     * Define la configuración común de la cadena de filtros.
     * Los microservicios deben llamar a este método y añadir sus rutas específicas.
     *
     * @param http Configuración de seguridad de Spring.
     * @return {@link HttpSecurity} configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Override
    protected HttpSecurity configurarAutorizacion(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // El endpoint de verificación de IP para el Gateway debe ser público o interno
                .requestMatchers("/api/v1/auditoria/seguridad/verificar-ip/**").permitAll()

                // Las consultas detalladas solo para administradores
                .requestMatchers(HttpMethod.GET,"/api/auditoria/accesos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/v1/auditoria/registros/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/v1/auditoria/accesos/**").hasRole("ADMIN")

                // El resto requiere autenticación
                .anyRequest().authenticated());
        return super.configurarAutorizacion(http);
    }
}

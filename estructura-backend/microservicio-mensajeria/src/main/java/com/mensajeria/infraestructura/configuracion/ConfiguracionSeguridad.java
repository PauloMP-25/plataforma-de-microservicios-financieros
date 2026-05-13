package com.mensajeria.infraestructura.configuracion;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de mensajería.
 * <p>
 * Permite el acceso a los endpoints de generación y validación de OTP, ya que
 * estos implementan su propia lógica de validación (throttling e identidad).
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt) {
        super(filtroJwt, puntoEntradaJwt);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return configurarAutorizacion(http).build();
    }

    @Override
    protected HttpSecurity configurarAutorizacion(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Endpoints de OTP son públicos (se validan internamente por UUID/Código)
                .requestMatchers("/api/v1/mensajeria/otp/**").permitAll()
                
                // Endpoints de administración (si se añaden en el futuro) requerirán ADMIN
                .requestMatchers("/api/v1/mensajeria/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated());

        return super.configurarAutorizacion(http);
    }
}

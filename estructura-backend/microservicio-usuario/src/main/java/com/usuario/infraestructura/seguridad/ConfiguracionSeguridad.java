package com.usuario.infraestructura.seguridad;

import com.libreria.comun.seguridad.ConfiguracionSeguridadBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de usuario.
 * Extiende de ConfiguracionSeguridadBase para heredar la configuración centralizada de la plataforma.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class ConfiguracionSeguridad extends ConfiguracionSeguridadBase {

    private final UserDetailsService userDetailsService;

    public ConfiguracionSeguridad(FiltroJwt filtroJwt, PuntoEntradaJwt puntoEntradaJwt, UserDetailsService userDetailsService) {
        super(filtroJwt, puntoEntradaJwt);
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configura la cadena de filtros de seguridad específica para el microservicio de usuario.
     * 
     * @param http Configuración HttpSecurity.
     * @return SecurityFilterChain configurado.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SEGURIDAD] Configurando seguridad para MS-USUARIO");
        
        // Usamos el método base para configurar la infraestructura central (Stateless, JWT, etc)
        super.configurarAutorizacion(http);

        // Añadimos las reglas específicas de este microservicio
        http.authorizeHttpRequests(auth -> auth
                // Endpoints Públicos de Autenticación
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/login",
                        "/api/v1/auth/registrar",
                        "/api/v1/auth/recuperar-solicitar",
                        "/api/v1/auth/recuperar-confirmar"
                ).permitAll()
                // Endpoint de Activación y Sincronización Interna
                .requestMatchers(HttpMethod.PUT, 
                        "/api/v1/auth/activar/**",
                        "/api/v1/datos-personales/**"
                ).permitAll()
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
        );

        // Inyectamos el proveedor de autenticación personalizado (DB)
        http.authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * Configura el proveedor de autenticación basado en base de datos.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder()); // Heredado de ConfiguracionSeguridadBase
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

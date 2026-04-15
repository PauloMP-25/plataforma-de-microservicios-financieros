package com.cliente.infraestructura.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracionSeguridad {

    private final FiltroJwt       filtroJwt;
    private final PuntoEntradaJwt puntoEntradaJwt;

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                    s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(puntoEntradaJwt))
            .authorizeHttpRequests(auth -> auth

                // Solo ADMIN o comunicación interna puede crear perfiles iniciales
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/clientes/inicial").permitAll()
                        //.hasAnyRole("ADMIN", "SYSTEM")

                // Consulta SUNAT: solo autenticados
                //.requestMatchers(HttpMethod.GET,
                //      "/api/v1/clientes/sunat/**")
                //        .authenticated()

                // Actualización y consulta de perfil: autenticados
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/clientes/completar/**").permitAll()
                        //.authenticated()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/clientes/perfil/**").permitAll()
                        //.authenticated()

                // Actuator
                .requestMatchers("/actuator/health").permitAll()

                .anyRequest().authenticated()
            )
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

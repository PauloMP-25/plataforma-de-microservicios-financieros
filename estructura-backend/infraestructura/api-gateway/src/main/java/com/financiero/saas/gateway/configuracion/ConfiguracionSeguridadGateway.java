package com.financiero.saas.gateway.configuracion;

import com.financiero.saas.gateway.seguridad.ServicioJwtGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuración de seguridad reactiva del API Gateway.
 *
 * ─── ¿Por qué @EnableWebFluxSecurity en vez de @EnableWebSecurity? ────────
 *
 * El Gateway usa Netty (servidor reactivo WebFlux), NO Tomcat (bloqueante).
 * Por eso:
 * - @EnableWebSecurity + HttpSecurity → para aplicaciones MVC/bloqueantes
 * - @EnableWebFluxSecurity + ServerHttpSecurity → para WebFlux/Gateway
 *
 * Si usas HttpSecurity aquí, Spring lanzará un error de contexto porque
 * no hay un DispatcherServlet, hay un DispatcherHandler reactivo.
 *
 * ─── ¿Qué hace esta configuración? ──────────────────────────────────────────
 *
 * La validación JWT real la hace FiltroJwtGlobal.
 * Esta clase solo:
 * 1. Deshabilita CSRF (innecesario en APIs REST stateless)
 * 2. Configura qué rutas son públicas a nivel de Security
 * 3. Maneja el 401 de forma consistente con el resto de la app
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ConfiguracionSeguridadGateway {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 1. Configuramos la base (Stateless, Sin sesiones)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // 2. Definimos las reglas del Gateway (De lo más específico a lo general)
                .authorizeExchange(exchanges -> exchanges
                        // Monitoreo y Documentación (Público)
                        .pathMatchers(
                                "/actuator/**",
                                "/error/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        // Endpoints Públicos de Negocio
                        .pathMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/mensajeria/otp/**")
                        .permitAll()

                        // 3. BLOQUEO TOTAL (Security lo permite porque FiltroJwtGlobal valida)
                        .anyExchange().permitAll())

                // Respuesta personalizada para 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    /**
     * KeyResolver para Rate Limiting.
     *
     * Determina qué "clave" identifica al usuario para el rate limiter.
     * Estrategia:
     * 1. Si hay token JWT → usar el usuarioId del claim (usuarios autenticados)
     * 2. Si no → usar la IP del cliente (peticiones públicas como login)
     *
     * Esto previene que un usuario autenticado consuma el cupo de otro,
     * y que ataques de fuerza bruta desde una IP no afecten a otros.
     * 
     * @param servicioJwt
     * @return
     */
    @Bean
    public KeyResolver userKeyResolver(ServicioJwtGateway servicioJwt) {
        return exchange -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String usuarioId = servicioJwt.extraerUsuarioId(token);
                    if (usuarioId != null) {
                        // Clave: "user:UUID" — identifica al usuario autenticado
                        return Mono.just("user:" + usuarioId);
                    }
                } catch (Exception ignored) {
                    // Token inválido → caer al fallback por IP
                }
            }

            // Fallback: usar IP del cliente
            @SuppressWarnings("null")
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}

package com.financiero.saas.gateway.filtros;

import com.financiero.saas.gateway.seguridad.ServicioJwtGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;

/**
 * Filtro de Cuota IA (Módulo 8 - Gobernanza).
 * Valida que el usuario no exceda su límite semanal de peticiones IA según su plan.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroCuotaIA implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ServicioJwtGateway servicioJwt;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Solo aplicar a rutas de IA
        if (!path.startsWith("/api/v1/ia")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange); // Delegar a FiltroJwtGlobal para el 401
        }

        String token = authHeader.substring(7);
        try {
            String usuarioId = servicioJwt.extraerUsuarioId(token);
            List<String> roles = servicioJwt.extraerRoles(token);
            String plan = determinarPlan(roles);

            int limite = obtenerLimitePorPlan(plan);
            String pool = path.contains("clasificar") ? "clasificacion" : "analitica";
            
            LocalDate ahora = LocalDate.now();
            int anio = ahora.getYear();
            int semana = ahora.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String key = String.format("ia:cuota:%s:%s:%d:W%d", pool, usuarioId, anio, semana);

            return redisTemplate.opsForValue().get(key)
                    .map(Integer::parseInt)
                    .defaultIfEmpty(0)
                    .flatMap(intentos -> {
                        if (intentos >= limite) {
                            log.warn("[CUOTA-IA] Límite excedido para usuario {}. Plan: {}, Pool: {}", usuarioId, plan, pool);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                        // Nota: El incremento real se hace en el Microservicio IA para evitar falsos positivos
                        // si el Gateway permite la entrada pero el MS-IA falla. 
                        // Sin embargo, para una protección "hard" en la puerta, podríamos incrementarlo aquí.
                        // Por ahora, solo validamos para cumplir con el requerimiento del usuario de "bloquear en la puerta".
                        return chain.filter(exchange);
                    });

        } catch (Exception e) {
            log.error("[CUOTA-IA] Error al validar cuota: {}", e.getMessage());
            return chain.filter(exchange); // Ante error, dejamos pasar para no romper el servicio
        }
    }

    private String determinarPlan(List<String> roles) {
        if (roles == null) return "FREE";
        if (roles.contains("ROLE_PREMIUM")) return "PREMIUM";
        if (roles.contains("ROLE_PRO")) return "PRO";
        return "FREE";
    }

    private int obtenerLimitePorPlan(String plan) {
        switch (plan) {
            case "PREMIUM": return 20;
            case "PRO": return 10;
            default: return 1; // FREE: 1/semana
        }
    }

    @Override
    public int getOrder() {
        // Ejecutar después de la validación JWT pero antes del enrutamiento
        return 0;
    }
}

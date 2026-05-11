package com.financiero.saas.gateway.filtros;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Filtro de seguridad "Muro de Fuego".
 * Verifica si la IP del cliente se encuentra bloqueada en Redis antes de enrutar la petición.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroBloqueoIp implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Boolean> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
        String key = "bloqueo:ip:" + ip;

        return redisTemplate.opsForValue().get(key)
                .flatMap(bloqueada -> {
                    if (Boolean.TRUE.equals(bloqueada)) {
                        log.warn("[MURO-FUEGO] Petición bloqueada para IP: {}", ip);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange)); // Si no está en Redis, permitir
    }

    @Override
    public int getOrder() {
        // Ejecutar muy temprano, antes de cualquier enrutamiento o lógica pesada
        return -100;
    }
}

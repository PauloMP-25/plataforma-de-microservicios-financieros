package com.financiero.saas.gateway.filtros;

import com.financiero.saas.gateway.seguridad.SeguridadClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filtro de seguridad perimetral "Muro de Fuego".
 * Verifica de forma reactiva y no bloqueante si la IP origen está bloqueada (fuerza bruta o manual)
 * utilizando el caché unificado en Redis o consultando asíncronamente al microservicio de auditoría.
 * Ejecuta en la máxima prioridad (HIGHEST_PRECEDENCE) para rechazar conexiones indeseadas
 * antes de procesar cualquier otra lógica (trazabilidad, JWT, cuotas, etc.).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroBloqueoIp implements GlobalFilter, Ordered {

    private final SeguridadClient seguridadClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = extraerIp(exchange.getRequest());

        return seguridadClient.estaBloqueada(ip)
                .flatMap(bloqueada -> {
                    if (Boolean.TRUE.equals(bloqueada)) {
                        log.warn("[MURO-FUEGO] Petición rechazada para IP bloqueada: {}", ip);
                        return responderError(exchange, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                "Acceso denegado por políticas de seguridad");
                    }
                    return chain.filter(exchange);
                });
    }

    private String extraerIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "IP_DESCONOCIDA";
    }

    @SuppressWarnings("null")
    private Mono<Void> responderError(ServerWebExchange exchange, HttpStatus status, String error, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Formato estándar de respuesta JSON compatible con ResultadoApi del backend
        String cuerpo = String.format(
                "{\"exito\": false, \"estado\": %d, \"error\": \"%s\", \"mensaje\": \"%s\", \"ruta\": \"%s\"}",
                status.value(), error, msg, exchange.getRequest().getURI().getPath());

        DataBuffer buffer = response.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Ejecutar en la máxima prioridad absoluta, garantizando que sea el primer filtro perimetral
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

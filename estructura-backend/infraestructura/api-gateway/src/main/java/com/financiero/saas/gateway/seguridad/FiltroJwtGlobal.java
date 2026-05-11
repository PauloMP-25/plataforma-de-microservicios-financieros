package com.financiero.saas.gateway.seguridad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.libreria.comun.enums.CodigoError;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filtro global perimetral para el API Gateway.
 * <p>
 * Implementa la validación de seguridad en dos fases:
 * 1. Verifica de forma reactiva si la IP origen está bloqueada por ataques.
 * 2. Valida y desencripta el token JWT para peticiones a rutas protegidas.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwtGlobal implements GlobalFilter, Ordered {

    private final ServicioJwtGateway servicioJwt;
    private final SeguridadClient seguridadClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String ipCliente = extraerIp(exchange.getRequest());

        log.debug("Verificando petición: [{} {}] desde IP: {}", exchange.getRequest().getMethod(), path, ipCliente);

        // 1. ¿IP BLOQUEADA? (Estrategia de Seguridad Robusta con Redis)
        return seguridadClient.estaBloqueada(ipCliente)
                .flatMap(bloqueada -> {
                    if (Boolean.TRUE.equals(bloqueada)) {
                        log.warn("[SEGURIDAD] Rechazando petición de IP bloqueada: {}", ipCliente);
                        return responderError(exchange, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                "Acceso denegado por políticas de seguridad");
                    }
                    return continuarValidacionJwt(exchange, chain, path);
                });
    }

    private Mono<Void> continuarValidacionJwt(ServerWebExchange exchange, GatewayFilterChain chain, String path) {
        // 2. RUTAS PÚBLICAS (Login, Registro)
        if (esRutaPublica(path)) {
            return chain.filter(exchange);
        }

        // 3. VALIDACIÓN USANDO LA LIBRERÍA
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responderError(exchange, CodigoError.ACCESO_NO_AUTORIZADO, "Token ausente");
        }

        String token = authHeader.substring(7);
        try {
            // Aquí usamos la lógica común de la librería
            if (!servicioJwt.esTokenValido(token)) {
                return responderError(exchange, CodigoError.TOKEN_INVALIDO, "Token no vigente");
            }
        } catch (Exception e) {
            return responderError(exchange, CodigoError.TOKEN_INVALIDO, "Error de firma");
        }

        // 4. INYECCIÓN DE HEADERS PARA LOS MICROS
        ServerHttpRequest requestModificada = exchange.getRequest().mutate()
                .header("X-Usuario-Id", servicioJwt.extraerUsuarioId(token).toString())
                .header("X-Usuario-Roles", String.join(",", servicioJwt.extraerRoles(token)))
                .build();

        return chain.filter(exchange.mutate().request(requestModificada).build());
    }

    @SuppressWarnings("null")
    private String extraerIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For puede contener múltiples IPs, la primera es el origen
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "IP_DESCONOCIDA";
    }

    // Usamos nuestro ResultadoApi de la librería para el error del Gateway
    private Mono<Void> responderError(ServerWebExchange exchange, CodigoError cod, String msg) {
        return responderError(exchange, cod.getStatus(), cod.name(), msg);
    }

    @SuppressWarnings("null")
    private Mono<Void> responderError(ServerWebExchange exchange, HttpStatus status, String error, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Creamos el formato de la librería manualmente porque Gateway es reactivo
        String cuerpo = String.format(
                "{\"exito\": false, \"estado\": %d, \"error\": \"%s\", \"mensaje\": \"%s\", \"ruta\": \"%s\"}",
                status.value(), error, msg, exchange.getRequest().getURI().getPath());

        DataBuffer buffer = response.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean esRutaPublica(String path) {
        return path.contains("/auth/") || path.contains("/v3/api-docs");
    }
}
package com.financiero.saas.gateway.seguridad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwtGlobal implements GlobalFilter, Ordered {

    private final ServicioJwtGateway servicioJwt;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String ipCliente = request.getRemoteAddress().getAddress().getHostAddress();

        log.debug("Verificando petición: [{} {}] desde IP: {}", request.getMethod(), path, ipCliente);

        // --- 1. ¿IP BLOQUEADA? (Estrategia de Seguridad Robusta) ---
        // TODO: Aquí llamaremos a un servicio que consulte Redis
        // if (servicioBloqueo.estaBloqueada(ipCliente)) { 
        //    return responderError(exchange, HttpStatus.FORBIDDEN, "IP_BLOQUEADA", "Demasiados intentos fallidos.");
        // }
        // --- 2. OBTENER METADATA DE LA RUTA ---
        Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (ruta == null) {
            return chain.filter(exchange);
        }

        Map<String, Object> metadata = ruta.getMetadata();
        boolean requiereAuth = (boolean) metadata.getOrDefault("requiere-autenticacion", true);

        // Si la ruta es pública, pasamos de largo
        if (!requiereAuth) {
            return chain.filter(exchange);
        }

        // --- 3. VALIDACIÓN DE TOKEN ---
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responderError(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_FALTANTE", "No se encontró el token de acceso.");
        }

        String token = authHeader.substring(7);

        if (!servicioJwt.esTokenValido(token)) {
            return responderError(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "Token expirado o corrupto.");
        }

        // --- 4. VALIDACIÓN DE ROLES ---
        String rolRequerido = (String) metadata.get("rol-requerido");
        if (rolRequerido != null) {
            List<String> rolesUsuario = servicioJwt.extraerRoles(token);
            if (rolesUsuario == null || !rolesUsuario.contains(rolRequerido)) {
                return responderError(exchange, HttpStatus.FORBIDDEN, "SIN_PERMISOS", "No tienes el rol: " + rolRequerido);
            }
        }

        // --- 5. INYECCIÓN DE HEADERS (Para los microservicios) ---
        // Mutamos la petición para que el microservicio final sepa quién es el usuario
        ServerHttpRequest requestModificada = request.mutate()
                .header("X-Usuario-Id", servicioJwt.extraerUsuarioId(token))
                .header("X-Usuario-Nombre", servicioJwt.extraerNombreUsuario(token))
                .header("X-Usuario-Roles", String.join(",", servicioJwt.extraerRoles(token)))
                .build();

        return chain.filter(exchange.mutate().request(requestModificada).build());
    }

    @Override
    public int getOrder() {
        // Ejecutar antes que otros filtros para ahorrar recursos si el token es malo
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // --- RESPUESTA DE ERROR ESTÁNDAR (JSON) ---
    private Mono<Void> responderError(ServerWebExchange exchange, HttpStatus status, String codigo, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String cuerpo = String.format(
                "{\"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"message\": \"%s\", \"path\": \"%s\"}",
                LocalDateTime.now(), status.value(), codigo, mensaje, exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}

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
        String path = exchange.getRequest().getURI().getPath();
        String ipCliente = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        log.debug("Verificando petición: [{} {}] desde IP: {}", request.getMethod(), path, ipCliente);

        // --- 1. ¿IP BLOQUEADA? (Estrategia de Seguridad Robusta) ---
        // TODO: Aquí llamaremos a un servicio que consulte Redis
        // if (servicioBloqueo.estaBloqueada(ipCliente)) { 
        //    return responderError(exchange, HttpStatus.FORBIDDEN, "IP_BLOQUEADA", "Demasiados intentos fallidos.");
        // }

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

    @Override
    private boolean esRutaPublica(String path) {
        return path.contains("/auth/") || path.contains("/v3/api-docs");
    }

    // Usamos nuestro ResultadoApi de la librería para el error del Gateway
    private Mono<Void> responderError(ServerWebExchange exchange, CodigoError cod, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(cod.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Creamos el formato de la librería manualmente porque Gateway es reactivo
        String cuerpo = String.format(
            "{\"exito\": false, \"estado\": %d, \"error\": \"%s\", \"mensaje\": \"%s\", \"ruta\": \"%s\"}",
            cod.getStatus().value(), cod.name(), msg, exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}


@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwtGlobal implements GlobalFilter, Ordered {

    // Usamos el servicio de la LIBRERÍA-COMUN
    private final ServicioJwt servicioJwt; 

}
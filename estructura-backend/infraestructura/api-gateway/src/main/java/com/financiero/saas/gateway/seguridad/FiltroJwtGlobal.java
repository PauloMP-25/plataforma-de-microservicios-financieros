package com.financiero.saas.gateway.seguridad;

import com.libreria.comun.enums.CodigoError;
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
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Filtro global perimetral para la validación y desencriptación del token JWT.
 * Se ejecuta únicamente después de que el cortafuegos de IPs (FiltroBloqueoIp)
 * haya validado que la IP de origen es segura.
 * 
 * @author Paulo Moron
 * @version 2.0.0
 * @since 2026-05-10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwtGlobal implements GlobalFilter, Ordered {

    private final ServicioJwtGateway servicioJwt;

    @Value("#{'${app.security.rutas-publicas}'.split(',')}")
    private List<String> rutasPublicas;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        log.debug("Verificando JWT para petición: [{} {}]", exchange.getRequest().getMethod(), path);

        // 1. RUTAS PÚBLICAS (Login, Registro)
        if (esRutaPublica(path)) {
            return chain.filter(exchange);
        }

        // 2. VALIDACIÓN USANDO LA LIBRERÍA
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

        // 3. INYECCIÓN DE HEADERS PARA LOS MICROSERVICIOS AGUAS ABAJO
        ServerHttpRequest requestModificada = exchange.getRequest().mutate()
                .header("X-Usuario-Id", servicioJwt.extraerUsuarioId(token))
                .header("X-Usuario-Roles", String.join(",", servicioJwt.extraerRoles(token)))
                .build();

        return chain.filter(exchange.mutate().request(requestModificada).build());
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
        // Se ejecuta después de FiltroBloqueoIp (Ordered.HIGHEST_PRECEDENCE) y FiltroTrazabilidadGlobal (Ordered.HIGHEST_PRECEDENCE + 1)
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private boolean esRutaPublica(String path) {
        if (rutasPublicas == null || rutasPublicas.isEmpty()) {
            return path.contains("/auth/") || path.contains("/v3/api-docs");
        }
        return rutasPublicas.stream().anyMatch(path::contains);
    }
}
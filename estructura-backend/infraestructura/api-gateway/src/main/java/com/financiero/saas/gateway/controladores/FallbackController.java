package com.financiero.saas.gateway.controladores;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controlador de respaldo (Fallback) para el API Gateway.
 * <p>
 * Este controlador es invocado por el filtro CircuitBreaker cuando un 
 * microservicio no responde o supera los umbrales de error.
 * Retorna una respuesta estandarizada siguiendo el contrato de ResultadoApi.
 */
@RestController
@Slf4j
public class FallbackController {

    @GetMapping("/fallback")
    public Mono<ResponseEntity<ResultadoApi<Void>>> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = "N/A";
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = (route != null) ? route.getId() : "desconocido";
        String originalUri = exchange.getRequest().getURI().getPath();

        Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        String exceptionMsg = (exception != null) ? exception.getMessage() : "Tiempo de espera agotado / Caída de servicio";

        log.error("[FALLBACK] Circuito abierto o error en ruta '{}'. RouteID: {}, X-Correlation-ID: {}, Causa: {}",
                originalUri, routeId, correlationId, exceptionMsg);

        ResultadoApi<Void> respuesta = ResultadoApi.falla(
                CodigoError.ERROR_SERVICIO_EXTERNO,
                "El servicio solicitado (" + routeId + ") no se encuentra disponible en este momento. Por favor, intente más tarde.",
                originalUri
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(respuesta));
    }
}

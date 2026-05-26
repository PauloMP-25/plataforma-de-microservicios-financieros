package com.financiero.saas.gateway.filtros;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro global para la gestión de la trazabilidad distribuida (Trace-ID).
 * <p>
 * Este filtro asegura que cada petición entrante tenga un identificador único
 * (X-Correlation-ID). Si el cliente no lo envía, el Gateway lo genera y lo
 * propaga hacia todos los microservicios aguas abajo.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
@Component
@Slf4j
public class FiltroTrazabilidadGlobal implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("[TRAZABILIDAD] Generando nuevo Trace-ID: {}", correlationId);
        } else {
            log.debug("[TRAZABILIDAD] Propagando Trace-ID existente: {}", correlationId);
        }

        // Inyectamos el ID en la petición que va hacia los microservicios
        ServerHttpRequest requestModificada = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        // También lo añadimos a la respuesta para facilitar el debugging en el cliente
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(requestModificada).build());
    }

    @Override
    public int getOrder() {
        // Debe ejecutarse antes que cualquier otro filtro (especialmente antes que el de seguridad)
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

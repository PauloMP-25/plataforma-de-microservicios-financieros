package com.financiero.saas.gateway.controladores;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controlador de respaldo (Fallback) para el API Gateway.
 * <p>
 * Este controlador es invocado por el filtro CircuitBreaker cuando un 
 * microservicio no responde o supera los umbrales de error.
 * Retorna una respuesta estandarizada siguiendo el contrato de ResultadoApi.
 */
@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public Mono<ResponseEntity<ResultadoApi<Void>>> fallback() {
        ResultadoApi<Void> respuesta = ResultadoApi.falla(
                CodigoError.ERROR_SERVICIO_EXTERNO,
                "El servicio solicitado no se encuentra disponible en este momento. Por favor, intente más tarde.",
                "/gateway/fallback"
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(respuesta));
    }
}

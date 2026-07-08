package com.cliente.infraestructura.clientes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cliente Feign para la comunicación saliente con el microservicio de Núcleo Financiero.
 * Permite integraciones de control financiero y aprovecha la configuración del circuit breaker de Feign.
 * 
 * @version 1.0.0
 * @since 2026-05-17
 */
@FeignClient(name = "microservicio-nucleo-financiero", url = "${URL_PROD_FINANCIERO:http://localhost:8085}")
public interface ClienteNucleoFinanciero {

    /**
     * Endpoint de salud para verificar conectividad con el Núcleo Financiero.
     * 
     * @return Estado del servicio
     */
    @GetMapping("/actuator/health")
    String verificarSalud();
}

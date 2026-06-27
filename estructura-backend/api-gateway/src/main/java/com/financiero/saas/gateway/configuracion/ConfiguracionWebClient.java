package com.financiero.saas.gateway.configuracion;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente HTTP reactivo para el API Gateway.
 * <p>
 * Define un constructor de WebClient,
 * utilizado para la comunicación HTTP asíncrona entre microservicios.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Configuration
public class ConfiguracionWebClient {

    /**
     * Crea un WebClient.Builder.
     * Este builder permite invocar a otros microservicios por su nombre
     * de servicio (ej: http://microservicio-auditoria).
     *
     * @return builder configurado para cliente HTTP
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}

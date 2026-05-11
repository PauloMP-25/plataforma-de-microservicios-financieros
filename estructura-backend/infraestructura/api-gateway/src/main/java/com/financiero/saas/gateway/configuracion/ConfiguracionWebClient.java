package com.financiero.saas.gateway.configuracion;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente HTTP reactivo para el API Gateway.
 * <p>
 * Define un constructor de WebClient balanceado por carga (Eureka),
 * necesario para la comunicación interna no bloqueante entre microservicios.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Configuration
public class ConfiguracionWebClient {

    /**
     * Proporciona un {@link WebClient.Builder} que soporta resolución de nombres
     * de servicio (ej: http://microservicio-auditoria) mediante Eureka.
     * 
     * @return Constructor de WebClient con soporte de balanceo de carga.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}

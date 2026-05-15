package com.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Clase principal del Microservicio de Pagos.
 * <p>
 * Gestiona la integración con Stripe, suscripciones y procesamiento de pagos
 * dentro del ecosistema LUKA APP.
 * </p>
 * 
 * @author Luka-Dev-Backend
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class MicroservicioPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioPagosApplication.class, args);
    }
}

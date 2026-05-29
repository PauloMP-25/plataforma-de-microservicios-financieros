package com.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pagos.infraestructura.configuracion.PropiedadesStripe;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
@SpringBootApplication(scanBasePackages = {"com.pagos", "com.libreria.comun"})
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(PropiedadesStripe.class)
public class MicroservicioPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioPagosApplication.class, args);
    }
}

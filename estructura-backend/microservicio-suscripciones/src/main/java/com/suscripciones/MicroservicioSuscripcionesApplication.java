package com.suscripciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Clase principal de inicio para el Microservicio de Suscripciones.
 */
@SpringBootApplication
@EnableFeignClients
public class MicroservicioSuscripcionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioSuscripcionesApplication.class, args);
    }
}

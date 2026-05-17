package com.cliente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal del Microservicio de Cliente.
 * Luka App - Gestiona perfiles, metas y limites globales.
 * Estandarizado según el modelo de microservicio-usuario.
 */
@SpringBootApplication(scanBasePackages = {"com.cliente", "com.libreria.comun"})
@EnableAsync
@EnableDiscoveryClient
@EnableFeignClients
public class MicroservicioClienteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioClienteApplication.class, args);
    }
}

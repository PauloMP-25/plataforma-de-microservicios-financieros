package com.cliente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Clase principal del Microservicio de Cliente.
 * Luka App - Gestiona perfiles, metas y limites globales.
 */

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories 
public class MicroservicioClienteApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioClienteApplication.class, args);
	}

}

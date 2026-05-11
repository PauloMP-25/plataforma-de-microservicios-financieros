package com.cliente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal del Microservicio de Cliente.
 * Luka App - Gestiona perfiles, metas y limites globales.
 */

@EnableDiscoveryClient
@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableJpaRepositories
@ComponentScan(basePackages = {
		"com.cliente",
		"com.libreria.comun"
})
public class MicroservicioClienteApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioClienteApplication.class, args);
	}

}

package com.nucleo.financiero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {
		"com.nucleo.financiero",
		"com.libreria.comun"
})
public class MicroservicioNucleoFinancieroApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioNucleoFinancieroApplication.class, args);
	}

}

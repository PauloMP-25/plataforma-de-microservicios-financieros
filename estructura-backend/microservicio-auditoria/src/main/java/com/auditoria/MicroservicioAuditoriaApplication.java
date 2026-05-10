package com.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "com.auditoria", 
    "com.libreria.comun" 
})
public class MicroservicioAuditoriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioAuditoriaApplication.class, args);
	}

}

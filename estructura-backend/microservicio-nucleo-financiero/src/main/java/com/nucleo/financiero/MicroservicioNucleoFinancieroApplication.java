package com.nucleo.financiero;

import com.nucleo.financiero.infraestructura.configuracion.PropiedadesJwt;
import com.nucleo.financiero.infraestructura.configuracion.PropiedadesInfraestructura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({PropiedadesJwt.class, PropiedadesInfraestructura.class})
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

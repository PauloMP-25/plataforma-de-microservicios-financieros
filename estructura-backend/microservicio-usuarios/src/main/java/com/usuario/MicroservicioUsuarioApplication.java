package com.usuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicroservicioUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioUsuarioApplication.class, args);
	}

}

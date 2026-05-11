package com.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal y punto de entrada para el microservicio de auditoría.
 * <p>
 * Este servicio forma parte del ecosistema de <b>Luka App</b> y tiene como
 * objetivo
 * centralizar, procesar y almacenar los eventos de auditoría (accesos y
 * transacciones)
 * distribuidos por toda la arquitectura de microservicios.
 * </p>
 * 
 * <p>
 * Configuraciones clave:
 * </p>
 * <ul>
 * <li>{@code @EnableDiscoveryClient}: Habilita el registro y descubrimiento en
 * el servidor Eureka.</li>
 * <li>{@code @EnableScheduling}: Permite la ejecución de tareas programadas,
 * como el procesamiento de colas o limpieza de logs.</li>
 * <li>{@code @ComponentScan}: Configurado para incluir los paquetes de la
 * {@code libreria-comun},
 * asegurando la inyección de DTOs y componentes compartidos.</li>
 * </ul>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
		"com.auditoria",
		"com.libreria.comun"
})
public class MicroservicioAuditoriaApplication {

	/**
	 * Punto de inicio de la aplicación.
	 * 
	 * @param args Argumentos de configuración pasados por línea de comandos durante
	 *             el arranque.
	 */
	public static void main(String[] args) {
		SpringApplication.run(MicroservicioAuditoriaApplication.class, args);
	}

}

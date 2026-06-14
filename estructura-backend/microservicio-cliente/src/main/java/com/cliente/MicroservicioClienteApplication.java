package com.cliente;

import com.cliente.infraestructura.configuracion.PropiedadesJwt;
import com.cliente.infraestructura.configuracion.PropiedadesInfraestructura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del Microservicio de Cliente.
 * Luka App - Gestiona perfiles, metas y limites globales.
 * Estandarizado según el modelo de microservicio-usuario.
 */
@SpringBootApplication(scanBasePackages = {"com.cliente", "com.libreria.comun"})
@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableConfigurationProperties({
        PropiedadesJwt.class,
        PropiedadesInfraestructura.class
})
public class MicroservicioClienteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioClienteApplication.class, args);
    }
}

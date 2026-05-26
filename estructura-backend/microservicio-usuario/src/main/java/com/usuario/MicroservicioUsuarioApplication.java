package com.usuario;

import com.usuario.infraestructura.configuracion.PropiedadesJwt;
import com.usuario.infraestructura.configuracion.PropiedadesInfraestructura;
import com.usuario.infraestructura.configuracion.PropiedadesBaseDatos;
import com.usuario.infraestructura.configuracion.PropiedadesRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.usuario", "com.libreria.comun"})
@EntityScan(basePackages = {"com.usuario", "com.libreria.comun"})
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties({
        PropiedadesJwt.class,
        PropiedadesInfraestructura.class,
        PropiedadesBaseDatos.class,
        PropiedadesRedis.class
})
public class MicroservicioUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioUsuarioApplication.class, args);
	}

}

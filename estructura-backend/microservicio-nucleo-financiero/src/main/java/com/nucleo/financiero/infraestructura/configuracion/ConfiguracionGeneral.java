package com.nucleo.financiero.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración general de beans utilitarios para el microservicio.
 * <p>
 * Define componentes compartidos como el RestTemplate para llamadas HTTP
 * externas fuera del ecosistema Feign.
 * </p>
 */
@Configuration
public class ConfiguracionGeneral {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

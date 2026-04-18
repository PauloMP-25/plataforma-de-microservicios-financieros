package com.mensajeria.infraestructura.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConfiguracionGeneral {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

package com.mensajeria.infraestructura.configuracion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada para la API de WhatsApp Cloud (Meta).
 * Mapea las propiedades con el prefijo 'luka.whatsapp'.
 */
@Configuration
@ConfigurationProperties(prefix = "luka.whatsapp")
@Data
public class WhatsAppConfig {
    private Api api = new Api();
    private Templates templates = new Templates();

    @Data
    public static class Api {
        private String url;
        private String phoneId;
        private String token;
    }

    @Data
    public static class Templates {
        private String registration;
        private String recovery;
        private String language;
    }
}

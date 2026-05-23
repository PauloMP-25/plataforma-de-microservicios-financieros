package com.mensajeria.infraestructura.configuracion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapeo centralizado de las propiedades de Twilio.
 */
@Configuration
@ConfigurationProperties(prefix = "twilio")
@Data
public class PropiedadesTwilio {
    
    private Account account = new Account();
    private Auth auth = new Auth();
    private ApiKey apiKey = new ApiKey();
    private Phone phone = new Phone();
    private Whatsapp whatsapp = new Whatsapp();

    @Data
    public static class Account {
        private String sid;
    }

    @Data
    public static class Auth {
        private String token;
    }

    @Data
    public static class ApiKey {
        private String sid;
        private String secret;
        private boolean useApiKey = false;
    }

    @Data
    public static class Phone {
        private String number;
    }

    @Data
    public static class Whatsapp {
        private String from;
    }
}

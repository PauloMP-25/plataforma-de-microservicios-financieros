package com.mensajeria.infraestructura.configuracion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapeo centralizado de las propiedades de Twilio.
 *
 * <p>A partir de la versión 1.2.0 se utiliza el {@code messagingServiceSid}
 * para enviar SMS y WhatsApp a través del Messaging Service de Twilio,
 * eliminando la dependencia de números de teléfono estáticos (from/number).
 * La autenticación se realiza con API Key Restringida (sid + secret) en lugar
 * del Auth Token maestro.</p>
 *
 * @author Paulo Moron
 * @version 1.2.0
 */
@Configuration
@ConfigurationProperties(prefix = "twilio")
@Data
public class PropiedadesTwilio {

    /** Account SID de la cuenta Twilio (requerido siempre). */
    private String accountSid;

    /** API Key SID de tipo Restricted con permisos de Messaging. */
    private String apiKeySid;

    /** API Key Secret asociado al {@code apiKeySid}. */
    private String apiKeySecret;

    /**
     * Messaging Service SID (prefijo {@code MG...}).
     * Permite enrutar mensajes sin fijar un número de origen estático.
     */
    private String messagingServiceSid;

    /** Número del Sandbox de WhatsApp de Twilio. */
    private String sandboxWhatsappFrom;

    // ── Clases internas para compatibilidad con binding de Spring Boot ──────────

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

    // ── Campos heredados (mantenidos por compatibilidad con TwilioInitializer) ──

    private Account account = new Account();
    private Auth auth = new Auth();
    private ApiKey apiKey = new ApiKey();
    private Phone phone = new Phone();
    private Whatsapp whatsapp = new Whatsapp();
}

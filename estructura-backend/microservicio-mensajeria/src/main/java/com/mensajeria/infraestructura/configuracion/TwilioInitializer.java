package com.mensajeria.infraestructura.configuracion;

import com.twilio.Twilio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Inicializador del SDK de Twilio en el arranque de la aplicación.
 * Permite alternar de forma transparente entre la autenticación tradicional
 * (Auth Token Maestro) y la recomendada para producción (API Keys Restringidas).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TwilioInitializer {

    private final PropiedadesTwilio propiedadesTwilio;

    @PostConstruct
    public void init() {
        try {
            if (propiedadesTwilio.getApiKey().isUseApiKey()) {
                String apiKeySid = propiedadesTwilio.getApiKey().getSid();
                String apiKeySecret = propiedadesTwilio.getApiKey().getSecret();
                String accountSid = propiedadesTwilio.getAccount().getSid();

                if (apiKeySid == null || apiKeySid.isBlank() ||
                    apiKeySecret == null || apiKeySecret.isBlank() ||
                    accountSid == null || accountSid.isBlank()) {
                    log.error("[TWILIO-INIT] Se configuró 'useApiKey=true' pero faltan credenciales requeridas (TWILIO_API_KEY_SID, TWILIO_API_KEY_SECRET o TWILIO_ACCOUNT_SID).");
                    return;
                }

                Twilio.init(apiKeySid, apiKeySecret, accountSid);
                log.info("[TWILIO-INIT] SDK de Twilio inicializado exitosamente usando API Key Restringida (SID: {}).", apiKeySid);
            } else {
                String accountSid = propiedadesTwilio.getAccount().getSid();
                String authToken = propiedadesTwilio.getAuth().getToken();

                if (accountSid == null || accountSid.isBlank() ||
                    authToken == null || authToken.isBlank()) {
                    log.warn("[TWILIO-INIT] Faltan credenciales del Auth Token Maestro de Twilio. El SDK se inicializará de forma tardía (lazy) si se configuran después.");
                    return;
                }

                Twilio.init(accountSid, authToken);
                log.info("[TWILIO-INIT] SDK de Twilio inicializado exitosamente usando Auth Token Maestro.");
            }
        } catch (Exception e) {
            log.error("[TWILIO-INIT] Error crítico al inicializar el SDK de Twilio: {}", e.getMessage());
        }
    }
}

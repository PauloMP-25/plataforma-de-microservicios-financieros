package com.mensajeria.aplicacion.servicios;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de SMS mediante Twilio.
 * Reutiliza la lógica de SmsService de Ikaza con inicialización lazy.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    private boolean twilioInitialized = false;

    // ─── Inicialización lazy del SDK ──────────────────────────────────────────

    private void initializeTwilio() {
        if (!twilioInitialized) {
            Twilio.init(accountSid, authToken);
            twilioInitialized = true;
            log.info("[SMS] Twilio inicializado correctamente");
        }
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    /**
     * Envía el código OTP por SMS al número indicado.
     *
     * @param telefono número destino en formato E.164 (+51XXXXXXXXX)
     * @param codigo   código de 6 dígitos
     */
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            initializeTwilio();

            String texto = String.format(
                "Tu código de verificación es: %s%nEste código es válido por 10 minutos.",
                codigo
            );
            Message message = Message.creator(
                    new PhoneNumber(telefono),
                    new PhoneNumber(fromPhoneNumber),
                    texto
            ).create();

            log.info("[SMS] Código OTP enviado a {}. SID: {}", telefono, message.getSid());

        } catch (Exception e) {
            log.error("[SMS] Error enviando SMS a {}: {}", telefono, e.getMessage());
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage());
        }
    }

    /**
     * Valida el formato E.164 del número de teléfono.
     * @param telefono
     * @return 
     */
    public boolean isValidPhoneNumber(String telefono) {
        return telefono != null && telefono.matches("^\\+[1-9]\\d{9,14}$");
    }
}
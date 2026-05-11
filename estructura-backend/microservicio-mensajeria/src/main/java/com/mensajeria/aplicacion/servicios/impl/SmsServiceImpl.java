package com.mensajeria.aplicacion.servicios.impl;

import com.mensajeria.aplicacion.servicios.ISmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementación concreta de {@link ISmsService} que usa el SDK de Twilio.
 * <p>
 * La inicialización del cliente Twilio es <em>lazy</em>: solo se realiza en el
 * primer envío real para no bloquear el arranque del servicio cuando las
 * variables de entorno no están configuradas en entorno local.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Service
@Slf4j
public class SmsServiceImpl implements ISmsService {

    /** SID de la cuenta Twilio, leído desde la variable de entorno {@code TWILIO_ACCOUNT_SID}. */
    @Value("${twilio.account.sid}")
    private String accountSid;

    /** Token de autenticación de Twilio, leído desde {@code TWILIO_AUTH_TOKEN}. */
    @Value("${twilio.auth.token}")
    private String authToken;

    /** Número de teléfono origen de Twilio en formato E.164. */
    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    private boolean twilioInitialized = false;

    /** Inicializa el SDK de forma perezosa para evitar errores de arranque en local. */
    private void initializeTwilio() {
        if (!twilioInitialized) {
            Twilio.init(accountSid, authToken);
            twilioInitialized = true;
            log.info("[SMS] Twilio inicializado.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param telefono Número destino en formato E.164 (ej. {@code +51987654321}).
     * @param codigo   Código OTP de 6 dígitos a enviar al usuario.
     */
    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            initializeTwilio();
            String texto = String.format(
                "Tu código de verificación LUKA es: %s%nVálido por 10 minutos. No lo compartas.",
                codigo
            );
            Message msg = Message.creator(
                    new PhoneNumber(telefono),
                    new PhoneNumber(fromPhoneNumber),
                    texto
            ).create();
            log.info("[SMS] OTP enviado a {}. SID: {}", telefono, msg.getSid());
        } catch (Exception e) {
            log.error("[SMS] Error enviando OTP a {}: {}", telefono, e.getMessage());
            throw new RuntimeException("Error al enviar SMS vía Twilio: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param telefono Número de teléfono a validar.
     * @return {@code true} si sigue el patrón {@code +[código_país][9-14 dígitos]}.
     */
    @Override
    public boolean esNumeroValido(String telefono) {
        return telefono != null && telefono.matches("^\\+[1-9]\\d{9,14}$");
    }
}

package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.puertos.ISmsService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;

import com.mensajeria.infraestructura.configuracion.PropiedadesTwilio;

/**
 * Implementación concreta de {@link ISmsService} que usa el SDK de Twilio.
 *
 * <p>A partir de la v1.2.0 el origen del mensaje se gestiona a través del
 * <strong>Messaging Service SID</strong> ({@code TWILIO_MESSAGING_SERVICE_SID}),
 * eliminando la dependencia de un número fijo. Twilio selecciona dinámicamente
 * el número de envío con la estrategia configurada en el Messaging Service.</p>
 *
 * @author Paulo Moron
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements ISmsService, CanalNotificacionStrategy {

    private final PropiedadesTwilio propiedadesTwilio;

    @Override
    public void enviar(String destinatario, java.util.Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        this.enviarCodigoVerificacion(destinatario, codigo);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.SMS;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utiliza {@code MessagingServiceSid} si está configurado (producción);
     * de lo contrario, cae de forma degradada al número estático {@code phone.number}
     * para entornos locales sin Messaging Service.</p>
     *
     * @param telefono Número destino en formato E.164 (ej. {@code +51987654321}).
     * @param codigo   Código OTP de 6 dígitos a enviar al usuario.
     */
    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            String texto = String.format(
                "Tu código de verificación LUKA es: %s%nVálido por 10 minutos. No lo compartas.",
                codigo
            );

            String messagingServiceSid = propiedadesTwilio.getMessagingServiceSid();
            Message msg;

            if (StringUtils.hasText(messagingServiceSid)) {
                // Modo producción: Messaging Service gestiona el número de origen
                log.info("[SMS] Usando MessagingServiceSid: {}", messagingServiceSid);
                msg = Message.creator(
                        new PhoneNumber(telefono),
                        messagingServiceSid,
                        texto
                ).create();
            } else {
                // Modo fallback: número estático (desarrollo local)
                String fromNumber = propiedadesTwilio.getPhone().getNumber();
                log.warn("[SMS] MessagingServiceSid no configurado. Usando número estático: {}", fromNumber);
                msg = Message.creator(
                        new PhoneNumber(telefono),
                        new PhoneNumber(fromNumber),
                        texto
                ).create();
            }
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

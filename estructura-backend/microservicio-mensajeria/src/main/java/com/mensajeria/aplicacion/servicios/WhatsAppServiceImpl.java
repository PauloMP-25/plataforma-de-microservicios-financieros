package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.puertos.IWhatsAppService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.mensajeria.infraestructura.configuracion.PropiedadesTwilio;

import java.util.Map;

/**
 * Implementación del servicio de WhatsApp usando Twilio.
 *
 * <p>A partir de la v1.2.0 el envío se realiza a través del
 * <strong>Messaging Service SID</strong> ({@code TWILIO_MESSAGING_SERVICE_SID}),
 * eliminando la dependencia de un número de WhatsApp fijo como origen.
 * Twilio gestiona el enrutamiento internamente.</p>
 *
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppServiceImpl implements IWhatsAppService, CanalNotificacionStrategy {

    private final PropiedadesTwilio propiedadesTwilio;

    @Override
    public void enviar(String destinatario, Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        this.sendVerificationCode(destinatario, codigo);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.WHATSAPP;
    }

    /**
     * Envía un código de verificación por WhatsApp aprovechando la ventana de 24 horas.
     *
     * <p>Utiliza {@code MessagingServiceSid} si está configurado (producción);
     * de lo contrario, cae de forma degradada al número estático {@code whatsapp.from}
     * para entornos locales sin Messaging Service.</p>
     *
     * @param targetPhone Número del cliente en formato internacional (ej: {@code +51943455686})
     * @param token       Código de verificación generado por el sistema
     * @return El SID del mensaje generado por Twilio si el envío fue exitoso
     */
    public String sendVerificationCode(String targetPhone, String token) {
        if (!esNumeroValido(targetPhone)) {
            log.error("[WHATSAPP] Formato de teléfono inválido: {}. Se requiere E.164 (ej. +51943455686)", targetPhone);
            throw new com.mensajeria.aplicacion.excepciones.TelefonoInvalidoException(
                "El número " + targetPhone + " no tiene el formato internacional requerido."
            );
        }

        try {
            String messageBody = String.format(
                "Luka App: Tu código de verificación es [%s]. Expira en 5 minutos. No lo compartas con nadie.",
                token
            );

            String formattedPhone = targetPhone.startsWith("+") ? targetPhone : "+" + targetPhone;
            // Twilio requiere el prefijo "whatsapp:" en el número destino
            String whatsappDest = "whatsapp:" + formattedPhone;

            String sandboxWhatsappFrom = propiedadesTwilio.getSandboxWhatsappFrom();
            String messagingServiceSid = propiedadesTwilio.getMessagingServiceSid();
            Message message;

            if (StringUtils.hasText(sandboxWhatsappFrom)) {
                String fromWhatsapp = "whatsapp:" + sandboxWhatsappFrom;
                log.info("[WHATSAPP] Usando Sandbox con remitente explícito: {}", fromWhatsapp);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        new PhoneNumber(fromWhatsapp),
                        messageBody
                ).create();
            } else if (StringUtils.hasText(messagingServiceSid)) {
                // Modo producción: Messaging Service gestiona el número de origen
                log.info("[WHATSAPP] Usando MessagingServiceSid: {}", messagingServiceSid);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        messagingServiceSid,
                        messageBody
                ).create();
            } else {
                // Modo fallback: número de WhatsApp estático (desarrollo local)
                String fromWhatsapp = "whatsapp:" + propiedadesTwilio.getWhatsapp().getFrom();
                log.warn("[WHATSAPP] MessagingServiceSid no configurado. Usando número estático: {}", fromWhatsapp);
                message = Message.creator(
                        new PhoneNumber(whatsappDest),
                        new PhoneNumber(fromWhatsapp),
                        messageBody
                ).create();
            }

            log.info("[WHATSAPP] Mensaje de verificación enviado con éxito. SID: {}", message.getSid());
            return message.getSid();

        } catch (Exception e) {
            log.error("[WHATSAPP] Error al enviar el token de validación por WhatsApp a {}: {}", targetPhone, e.getMessage());
            throw new MensajeriaExternaException("Fallo en el envío de la notificación de seguridad por WhatsApp", e.getMessage());
        }
    }

    @Override
    public void enviarMensajeTemplate(String telefono, String plantilla, Map<String, String> variables) {
        // Como ya no se usan plantillas, redirigimos la lógica a sendVerificationCode.
        // Asumimos que el token es la primera variable (ej. "1").
        String token = variables != null ? variables.getOrDefault("1", "DESCONOCIDO") : "DESCONOCIDO";
        this.sendVerificationCode(telefono, token);
    }

    @Override
    public boolean esNumeroValido(String telefono) {
        // WhatsApp requiere formato E.164 (ej: +51943455686) para Twilio
        return telefono != null && telefono.matches("^\\+?[1-9]\\d{9,14}$");
    }
}

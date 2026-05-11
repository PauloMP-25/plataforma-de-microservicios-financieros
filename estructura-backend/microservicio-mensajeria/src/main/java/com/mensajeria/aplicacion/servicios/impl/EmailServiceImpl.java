package com.mensajeria.aplicacion.servicios.impl;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.servicios.NotificacionService;
import com.mensajeria.aplicacion.servicios.TipoNotificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Implementación unificada de {@link NotificacionService} que maneja tanto
 * correos electrónicos (Thymeleaf + SMTP) como SMS (Twilio).
 * <p>
 * La selección del canal se delega al parámetro {@link TipoNotificacion}:
 * {@code EMAIL} construye un correo HTML con la plantilla correspondiente al
 * propósito del OTP, mientras que {@code SMS} invoca al SDK de Twilio de forma
 * perezosa para evitar errores en entornos locales sin credenciales.
 * </p>
 * <p>
 * El mapa {@code variables} debe contener al menos las claves
 * {@code "codigo"} y {@code "proposito"}. Para {@code EMAIL}, también se usa
 * {@code "appName"} en el asunto y el header de la plantilla.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Service
@Slf4j
public class EmailServiceImpl implements NotificacionService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /** Dirección de origen para los correos, leída de {@code spring.mail.username}. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /** Nombre de la app que aparece en asuntos y headers de correo. */
    @Value("${email.nombre.empresa:Luka App}")
    private String appName;

    /** SID de la cuenta Twilio. */
    @Value("${twilio.account.sid:}")
    private String accountSid;

    /** Token de autenticación de Twilio. */
    @Value("${twilio.auth.token:}")
    private String authToken;

    /** Número de teléfono origen de Twilio en formato E.164. */
    @Value("${twilio.phone.number:}")
    private String fromPhoneNumber;

    private boolean twilioInitialized = false;

    /**
     * Construye el servicio inyectando las dependencias de email.
     * Twilio se inicializa de forma perezosa en el primer envío SMS.
     *
     * @param mailSender     Sender SMTP configurado por Spring Boot Mail.
     * @param templateEngine Motor de plantillas Thymeleaf para generar HTML.
     */
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delega al canal correspondiente según el tipo:
     * <ul>
     * <li>{@link TipoNotificacion#EMAIL} → {@link #enviarEmail(String, Map)}</li>
     * <li>{@link TipoNotificacion#SMS} → {@link #enviarSms(String, Map)}</li>
     * </ul>
     * </p>
     *
     * @param tipo         Canal de entrega de la notificación.
     * @param destinatario Email (RFC 5321) o número de teléfono (E.164).
     * @param variables    Mapa con {@code "codigo"}, {@code "proposito"} y
     *                     opcionalmente
     *                     {@code "appName"}.
     */
    @Override
    @SuppressWarnings("null")
    public void enviar(TipoNotificacion tipo, String destinatario, Map<String, Object> variables) {
        switch (tipo) {
            case EMAIL -> enviarEmail(destinatario, variables);
            case SMS   -> enviarSms(destinatario, variables);
        }
    }

    // ── Email ──────────────────────────────────────────────────────────────────

    /**
     * Construye y envía el correo HTML seleccionando la plantilla Thymeleaf
     * según el {@code PropositoCodigo} contenido en el mapa de variables.
     *
     * @param email     Dirección de correo electrónico del destinatario.
     * @param variables Mapa con {@code "codigo"} y {@code "proposito"} (requeridos).
     * @throws MensajeriaExternaException si JavaMailSender no puede entregar el mensaje.
     */
    @SuppressWarnings("null")
    private void enviarEmail(String email, Map<String, Object> variables) {
        PropositoCodigo proposito = (PropositoCodigo) variables.get("proposito");
        String codigo = (String) variables.get("codigo");
        String nombreApp = (String) variables.getOrDefault("appName", appName);

        try {
            String plantilla = resolverPlantilla(proposito);
            String asunto = resolverAsunto(proposito, nombreApp);

            Context ctx = new Context();
            ctx.setVariable("codigo", codigo);
            ctx.setVariable("appName", nombreApp);
            String cuerpoHtml = templateEngine.process(plantilla, ctx);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(fromEmail, nombreApp);
            helper.setTo(email);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);

            mailSender.send(mensaje);
            log.info("[EMAIL] OTP enviado a '{}' — propósito: '{}'", email, proposito);

        } catch (MailException | MessagingException e) {
            log.error("[EMAIL] Fallo al enviar OTP a '{}': {}", email, e.getMessage());
            throw new MensajeriaExternaException(
                "No se pudo enviar el correo de verificación. Por favor, inténtalo más tarde.",
                e.getMessage()
            );
        } catch (Exception e) {
            log.error("[EMAIL] Error procesando plantilla Thymeleaf: {}", e.getMessage());
            throw new MensajeriaExternaException("Error interno al generar el correo.", e.getMessage());
        }
    }

    // ── SMS ────────────────────────────────────────────────────────────────────

    /**
     * Inicializa Twilio de forma perezosa y envía el SMS con el OTP.
     *
     * @param telefono  Número destino en formato E.164 (ej. {@code +51987654321}).
     * @param variables Mapa con la clave {@code "codigo"} (requerida).
     * @throws MensajeriaExternaException si Twilio rechaza el envío.
     */
    private void enviarSms(String telefono, Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        try {
            if (!twilioInitialized) {
                Twilio.init(accountSid, authToken);
                twilioInitialized = true;
            }
            String texto = String.format(
                "Tu código de verificación LUKA es: %s%nVálido 10 min. No lo compartas.", codigo);
            Message msg = Message.creator(
                    new PhoneNumber(telefono),
                    new PhoneNumber(fromPhoneNumber),
                    texto
            ).create();
            log.info("[SMS] OTP enviado a {}. SID: {}", telefono, msg.getSid());
        } catch (Exception e) {
            log.error("[SMS] Error enviando OTP a {}: {}", telefono, e.getMessage());
            throw new MensajeriaExternaException("Error al enviar SMS vía Twilio.", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String resolverPlantilla(PropositoCodigo proposito) {
        return switch (proposito) {
            case ACTIVACION_CUENTA    -> "activacion-cuenta";
            case RESTABLECER_PASSWORD -> "recuperacion-password";
        };
    }

    private String resolverAsunto(PropositoCodigo proposito, String nombre) {
        return switch (proposito) {
            case ACTIVACION_CUENTA    -> nombre + " — Activa tu cuenta";
            case RESTABLECER_PASSWORD -> nombre + " — Restablece tu contraseña";
        };
    }
}

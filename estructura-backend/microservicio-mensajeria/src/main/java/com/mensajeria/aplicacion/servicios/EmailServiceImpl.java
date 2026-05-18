package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.puertos.IEmailService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.libreria.comun.enums.PropositoCodigo;
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
 * Implementación de {@link IEmailService} para el envío de correos vía SMTP.
 */
@Service
@Slf4j
public class EmailServiceImpl implements IEmailService, CanalNotificacionStrategy {

    @Override
    public void enviar(String destinatario, Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        PropositoCodigo proposito = (PropositoCodigo) variables.get("proposito");
        this.enviarEmail(destinatario, proposito, codigo, variables);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.EMAIL;
    }

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.nombre.empresa:Luka App}")
    private String appName;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void enviarEmail(String email, PropositoCodigo proposito, String codigo, Map<String, Object> variables) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email destino no puede ser nulo o vacío.");
        }
        if (proposito == null) {
            throw new IllegalArgumentException("El propósito no puede ser nulo.");
        }
        
        String nombreApp = variables != null ? (String) variables.getOrDefault("appName", appName) : appName;

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
                    "No se pudo enviar el correo de verificación.", e.getMessage());
        } catch (Exception e) {
            log.error("[EMAIL] Error procesando plantilla Thymeleaf: {}", e.getMessage());
            throw new MensajeriaExternaException("Error interno al generar el correo.", e.getMessage());
        }
    }

    @Override
    public void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede ser nulo o vacío.");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(fromEmail, appName);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpo, esHtml);

            mailSender.send(mensaje);
            log.info("[EMAIL-ADMIN] Alerta enviada a '{}'", destinatario);
        } catch (Exception e) {
            log.error("[EMAIL-ADMIN] Fallo al enviar alerta a '{}': {}", destinatario, e.getMessage());
            throw new MensajeriaExternaException("No se pudo enviar el correo de alerta.", e.getMessage());
        }
    }

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

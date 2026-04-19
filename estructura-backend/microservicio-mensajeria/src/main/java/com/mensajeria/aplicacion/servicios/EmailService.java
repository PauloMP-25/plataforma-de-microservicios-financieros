package com.mensajeria.aplicacion.servicios;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos electrónicos. Reutiliza la lógica de
 * EmailService de Ikaza adaptada al contexto OTP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.nombre.empresa:MI_APP}")
    private String appName;

    // ─── API pública ─────────────────────────────────────────────────────────
    /**
     * Envía el código OTP de 6 dígitos al email indicado.
     *
     * @param email destinatario
     * @param codigo código de 6 dígitos a enviar
     */
    public void enviarCodigoVerificacion(String email, String codigo) {
        try {
            log.info("[EMAIL] Enviando código OTP a: {}", email);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Código de Verificación — " + appName);
            helper.setText(construirHtmlCodigoVerificacion(email, codigo), true);

            mailSender.send(message);
            log.info("[EMAIL] Código OTP enviado exitosamente a {}", email);

        } catch (MessagingException | MailException e) {
            log.error("[EMAIL] Error al enviar código OTP a {}: {}", email, e.getMessage());
            throw new RuntimeException("Error al enviar el código de verificación: " + e.getMessage());
        }
    }

    // ─── HTML Builder ─────────────────────────────────────────────────────────
    private String construirHtmlCodigoVerificacion(String email, String codigo) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head><body>
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">

              <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                          color: white; padding: 30px; text-align: center;">
                <h1 style="margin: 0;">🔐 Verificación de Cuenta</h1>
                <p style="margin: 10px 0 0 0; font-size: 16px;">%s</p>
              </div>

              <div style="padding: 40px; background-color: white; text-align: center;">
                <p style="color: #333; font-size: 16px; margin-bottom: 30px;">
                  Tu código de verificación es:
                </p>
                <div style="background-color: #f8f9fa; padding: 20px;
                             border-radius: 10px; margin: 20px 0;">
                  <h1 style="color: #667eea; font-size: 48px;
                              letter-spacing: 10px; margin: 0;">%s</h1>
                </div>
                <p style="color: #6c757d; font-size: 14px; margin-top: 30px;">
                  Este código es válido por <strong>10 minutos</strong>.
                </p>
                <p style="color: #6c757d; font-size: 14px;">
                  Si no solicitaste este código, puedes ignorar este mensaje.
                </p>
              </div>

              <div style="padding: 20px; text-align: center;
                           background-color: #343a40; color: white;">
                <p style="margin: 0; font-size: 14px;">%s — Sistema de Verificación</p>
                <p style="margin: 10px 0 0 0; font-size: 12px; color: #adb5bd;">
                  Este es un correo automático, por favor no responder.
                </p>
              </div>

            </div></body></html>
            """.formatted(appName, codigo, appName);
    }
}

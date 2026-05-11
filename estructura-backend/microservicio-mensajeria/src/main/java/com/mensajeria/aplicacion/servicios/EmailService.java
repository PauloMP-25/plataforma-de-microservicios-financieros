package com.mensajeria.aplicacion.servicios;

import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.nombre.empresa:MI_APP}")
    private String appName;

    /**
     * Envía el código OTP adaptando el asunto y cuerpo según el propósito.
     * @param email
     * @param codigo
     * @param proposito
     */
    public void enviarCodigoOtp(String email, String codigo, PropositoCodigo proposito) {
        try {
            String asunto = (proposito == PropositoCodigo.ACTIVACION_CUENTA)
                    ? "Verificación de Cuenta — " + appName
                    : "Restablecer Contraseña — " + appName;

            String tituloHtml = (proposito == PropositoCodigo.ACTIVACION_CUENTA)
                    ? "Verificación de Cuenta"
                    : "Restablecer Contraseña";

            enviarMimeMessage(email, asunto, construirHtml(tituloHtml, codigo));
            log.info("[EMAIL] OTP de {} enviado a {}", proposito, email);

        } catch (MessagingException | MailException e) {
            log.error("[EMAIL] Error al enviar OTP a {}: {}", email, e.getMessage());
            throw new RuntimeException("Error en el despacho de correo: " + e.getMessage());
        }
    }

    private void enviarMimeMessage(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    private String construirHtml(String titulo, String codigo) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head><body style="margin:0;padding:0;">
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee;">
              <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center;">
                <h1 style="margin: 0;">%s</h1>
                <p style="margin: 10px 0 0 0;">%s</p>
              </div>
              <div style="padding: 40px; text-align: center;">
                <p style="color: #333; font-size: 16px;">Tu código de seguridad es:</p>
                <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0;">
                  <h1 style="color: #667eea; font-size: 48px; letter-spacing: 10px; margin: 0;">%s</h1>
                </div>
                <p style="color: #6c757d; font-size: 14px;">Válido por 10 minutos. Si no lo solicitaste, ignora este correo.</p>
              </div>
              <div style="padding: 20px; text-align: center; background-color: #343a40; color: #adb5bd; font-size: 12px;">
                <p style="margin: 0;">%s — Seguridad Integral</p>
              </div>
            </div></body></html>
            """.formatted(titulo, appName, codigo, appName);
    }
}
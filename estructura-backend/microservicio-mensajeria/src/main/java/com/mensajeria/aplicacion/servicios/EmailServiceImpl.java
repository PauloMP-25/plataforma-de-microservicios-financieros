package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.puertos.IEmailService;
import com.mensajeria.aplicacion.servicios.canales.CanalNotificacionStrategy;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import com.libreria.comun.enums.PropositoCodigo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mensajeria.infraestructura.configuracion.PropiedadesEmail;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IEmailService} para el envío de correos vía SMTP.
 * <p>
 * Gestiona dos grupos de flujos completamente independientes:
 * <ul>
 *   <li>OTP (activación de cuenta, recuperación de contraseña) — usando plantillas Thymeleaf.</li>
 *   <li>Soporte al cliente (contacto y reporte de problemas) — HTML inline, sin plantillas.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class EmailServiceImpl implements IEmailService, CanalNotificacionStrategy {

    // ── Tipos MIME permitidos para adjuntos de soporte ──────────────────────
    private static final List<String> TIPOS_MIME_PERMITIDOS = List.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB

    private final JavaMailSender   mailSender;
    private final TemplateEngine   templateEngine;
    private final PropiedadesEmail propiedadesEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            PropiedadesEmail propiedadesEmail) {
        this.mailSender        = mailSender;
        this.templateEngine    = templateEngine;
        this.propiedadesEmail  = propiedadesEmail;
    }

    // =========================================================================
    // CanalNotificacionStrategy — delegación OTP
    // =========================================================================

    @Override
    public void enviar(String destinatario, Map<String, Object> variables) {
        String       codigo    = (String)       variables.get("codigo");
        PropositoCodigo proposito = (PropositoCodigo) variables.get("proposito");
        this.enviarEmail(destinatario, proposito, codigo, variables);
    }

    @Override
    public boolean soporta(TipoNotificacion tipo) {
        return tipo == TipoNotificacion.EMAIL;
    }

    // =========================================================================
    // OTP — flujos existentes (NO modificar)
    // =========================================================================

    @Override
    public void enviarEmail(String email, PropositoCodigo proposito, String codigo,
                            Map<String, Object> variables) {
        if (email == null || email.isBlank())    throw new IllegalArgumentException("El email destino no puede ser nulo o vacío.");
        if (proposito == null)                   throw new IllegalArgumentException("El propósito no puede ser nulo.");

        String appNameConfig = propiedadesEmail.getNombre().getEmpresa();
        String nombreApp = variables != null
                ? (String) variables.getOrDefault("appName", appNameConfig)
                : appNameConfig;

        try {
            String plantilla   = resolverPlantilla(proposito);
            String asunto      = resolverAsunto(proposito, nombreApp);
            Context ctx        = new Context();
            ctx.setVariable("codigo",  codigo);
            ctx.setVariable("appName", nombreApp);
            String cuerpoHtml  = templateEngine.process(plantilla, ctx);

            MimeMessage       mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper  = new MimeMessageHelper(mensaje, true, "UTF-8");
            String origen = propiedadesEmail.getFrom() != null ? propiedadesEmail.getFrom() : "noreply@luka.com";
            helper.setFrom(origen, nombreApp);
            helper.setTo(email);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(mensaje);
            log.info("[EMAIL] OTP enviado a '{}' — propósito: '{}'", email, proposito);

        } catch (MailException | MessagingException e) {
            log.error("[EMAIL] Fallo al enviar OTP a '{}': {}", email, e.getMessage());
            throw new MensajeriaExternaException("No se pudo enviar el correo de verificación.", e.getMessage());
        } catch (Exception e) {
            log.error("[EMAIL] Error procesando plantilla Thymeleaf: {}", e.getMessage());
            throw new MensajeriaExternaException("Error interno al generar el correo.", e.getMessage());
        }
    }

    @Override
    public void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml) {
        if (destinatario == null || destinatario.isBlank())
            throw new IllegalArgumentException("El destinatario no puede ser nulo o vacío.");
        try {
            MimeMessage       mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper  = new MimeMessageHelper(mensaje, true, "UTF-8");
            String origen = propiedadesEmail.getFrom() != null ? propiedadesEmail.getFrom() : "noreply@luka.com";
            helper.setFrom(origen, propiedadesEmail.getNombre().getEmpresa());
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

    // =========================================================================
    // Soporte al cliente — flujos nuevos (independientes del OTP)
    // =========================================================================

    @Override
    public void enviarEmailSoporte(String asunto, String categoria, String mensaje,
                                   MultipartFile adjunto) {
        String destinatario = resolverDestinatarioSoporte();

        try {
            MimeMessage       mime   = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            String origen    = propiedadesEmail.getFrom() != null ? propiedadesEmail.getFrom() : "noreply@luka.com";
            String nombreApp = propiedadesEmail.getNombre().getEmpresa();

            helper.setFrom(origen, nombreApp);
            helper.setTo(destinatario);
            helper.setSubject("[Soporte Luka] [" + categoria + "] " + asunto);
            helper.setText(construirHtmlSoporte(asunto, categoria, mensaje), true);

            if (adjunto != null && !adjunto.isEmpty()) {
                validarAdjunto(adjunto);
                helper.addAttachment(adjunto.getOriginalFilename() != null
                        ? adjunto.getOriginalFilename() : "adjunto", adjunto);
            }

            mailSender.send(mime);
            log.info("[EMAIL-SOPORTE] Consulta de soporte enviada a '{}' — categoría: '{}'",
                    destinatario, categoria);

        } catch (MailException | MessagingException e) {
            log.error("[EMAIL-SOPORTE] Fallo al enviar consulta: {}", e.getMessage());
            throw new MensajeriaExternaException("No se pudo enviar la consulta de soporte.", e.getMessage());
        } catch (MensajeriaExternaException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EMAIL-SOPORTE] Error inesperado: {}", e.getMessage());
            throw new MensajeriaExternaException("Error interno al procesar la consulta de soporte.", e.getMessage());
        }
    }

    @Override
    public void enviarEmailReporte(String descripcionCorta, String prioridad, String seccion,
                                   String descripcion, String queHacias, MultipartFile adjunto) {
        String destinatario = resolverDestinatarioSoporte();

        try {
            MimeMessage       mime   = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            String origen    = propiedadesEmail.getFrom() != null ? propiedadesEmail.getFrom() : "noreply@luka.com";
            String nombreApp = propiedadesEmail.getNombre().getEmpresa();

            helper.setFrom(origen, nombreApp);
            helper.setTo(destinatario);
            helper.setSubject("[Reporte Luka] [" + prioridad.toUpperCase() + "] " + descripcionCorta);
            helper.setText(construirHtmlReporte(descripcionCorta, prioridad, seccion, descripcion, queHacias), true);

            if (adjunto != null && !adjunto.isEmpty()) {
                validarAdjunto(adjunto);
                helper.addAttachment(adjunto.getOriginalFilename() != null
                        ? adjunto.getOriginalFilename() : "evidencia", adjunto);
            }

            mailSender.send(mime);
            log.info("[EMAIL-REPORTE] Reporte enviado a '{}' — prioridad: '{}', sección: '{}'",
                    destinatario, prioridad, seccion);

        } catch (MailException | MessagingException e) {
            log.error("[EMAIL-REPORTE] Fallo al enviar reporte: {}", e.getMessage());
            throw new MensajeriaExternaException("No se pudo enviar el reporte de problema.", e.getMessage());
        } catch (MensajeriaExternaException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EMAIL-REPORTE] Error inesperado: {}", e.getMessage());
            throw new MensajeriaExternaException("Error interno al procesar el reporte.", e.getMessage());
        }
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Resuelve el destinatario de soporte desde la propiedad {@code email.soporte}.
     * Falla rápido si no está configurado para evitar envíos silenciosos a null.
     */
    private String resolverDestinatarioSoporte() {
        String soporte = propiedadesEmail.getSoporte();
        if (soporte == null || soporte.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad 'email.soporte' no está configurada. " +
                    "Defínela en application.yml o mediante la variable de entorno EMAIL_SOPORTE.");
        }
        return soporte;
    }

    /**
     * Valida tipo MIME y tamaño del archivo adjunto.
     * Lanza {@link MensajeriaExternaException} si no cumple los requisitos.
     */
    private void validarAdjunto(MultipartFile adjunto) {
        String tipo = adjunto.getContentType();
        if (tipo == null || !TIPOS_MIME_PERMITIDOS.contains(tipo)) {
            throw new MensajeriaExternaException(
                    "Tipo de archivo no permitido. Solo se aceptan: JPG, PNG, WEBP.",
                    "Tipo recibido: " + tipo);
        }
        if (adjunto.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new MensajeriaExternaException(
                    "El archivo adjunto supera el tamaño máximo permitido de 5 MB.",
                    "Tamaño recibido: " + adjunto.getSize() + " bytes");
        }
    }

    /** Construye el HTML del correo de soporte (sin plantilla Thymeleaf). */
    private String construirHtmlSoporte(String asunto, String categoria, String mensaje) {
        String nombreApp = propiedadesEmail.getNombre().getEmpresa();
        return """
                <html><body style="font-family:Arial,sans-serif;color:#1a1a2e;background:#f5f5f5;padding:32px">
                  <div style="max-width:620px;margin:0 auto;background:#fff;border-radius:12px;padding:36px;border:1px solid #e0e0e0">
                    <h2 style="color:#5b6af0;margin-top:0">📩 Nueva consulta de soporte</h2>
                    <table style="width:100%;border-collapse:collapse">
                      <tr><td style="padding:8px 0;font-weight:700;width:130px;color:#555">Asunto:</td>
                          <td style="padding:8px 0">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:700;color:#555">Categoría:</td>
                          <td style="padding:8px 0"><span style="background:#ede9fe;color:#5b6af0;padding:3px 10px;border-radius:20px;font-size:0.85rem">%s</span></td></tr>
                    </table>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <h3 style="color:#333;margin-top:0">Mensaje del usuario:</h3>
                    <p style="background:#f9f9f9;padding:16px;border-radius:8px;line-height:1.7;color:#444">%s</p>
                    <p style="color:#888;font-size:0.8rem;margin-top:24px">Este correo fue generado automáticamente por %s.</p>
                  </div>
                </body></html>
                """.formatted(asunto, categoria, mensaje.replace("\n", "<br>"), nombreApp);
    }

    /** Construye el HTML del correo de reporte de problema (sin plantilla Thymeleaf). */
    private String construirHtmlReporte(String descripcionCorta, String prioridad,
                                        String seccion, String descripcion, String queHacias) {
        String nombreApp  = propiedadesEmail.getNombre().getEmpresa();
        String colorPrio  = switch (prioridad.toLowerCase()) {
            case "alta"  -> "#ef4444";
            case "media" -> "#f59e0b";
            default      -> "#22c55e";
        };
        String filaQueHacias = (queHacias != null && !queHacias.isBlank())
                ? "<tr><td style=\"padding:8px 0;font-weight:700;color:#555\">¿Qué hacías?:</td>"
                  + "<td style=\"padding:8px 0\">" + queHacias.replace("\n", "<br>") + "</td></tr>"
                : "";

        return """
                <html><body style="font-family:Arial,sans-serif;color:#1a1a2e;background:#f5f5f5;padding:32px">
                  <div style="max-width:620px;margin:0 auto;background:#fff;border-radius:12px;padding:36px;border:1px solid #e0e0e0">
                    <h2 style="color:#f59e0b;margin-top:0">⚠️ Reporte de problema</h2>
                    <table style="width:100%;border-collapse:collapse">
                      <tr><td style="padding:8px 0;font-weight:700;width:130px;color:#555">Problema:</td>
                          <td style="padding:8px 0;font-weight:600">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:700;color:#555">Prioridad:</td>
                          <td style="padding:8px 0">
                            <span style="background:%s22;color:%s;padding:3px 12px;border-radius:20px;font-size:0.85rem;font-weight:700">%s</span>
                          </td></tr>
                      <tr><td style="padding:8px 0;font-weight:700;color:#555">Sección:</td>
                          <td style="padding:8px 0">%s</td></tr>
                      %s
                    </table>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0">
                    <h3 style="color:#333;margin-top:0">Descripción del problema:</h3>
                    <p style="background:#f9f9f9;padding:16px;border-radius:8px;line-height:1.7;color:#444">%s</p>
                    <p style="color:#888;font-size:0.8rem;margin-top:24px">Este reporte fue generado automáticamente por %s.</p>
                  </div>
                </body></html>
                """.formatted(descripcionCorta, colorPrio, colorPrio, prioridad,
                              seccion, filaQueHacias,
                              descripcion.replace("\n", "<br>"), nombreApp);
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

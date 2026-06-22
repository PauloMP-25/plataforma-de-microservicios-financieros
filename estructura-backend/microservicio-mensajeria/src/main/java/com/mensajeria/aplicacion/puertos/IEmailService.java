package com.mensajeria.aplicacion.puertos;

import com.libreria.comun.enums.PropositoCodigo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Contrato para el envío de correos electrónicos.
 */
public interface IEmailService {

    // ── OTP ──────────────────────────────────────────────────────────────────
    void enviarEmail(String email, PropositoCodigo proposito, String codigo, Map<String, Object> variables);

    void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml);

    // ── Soporte al cliente ────────────────────────────────────────────────────

    /**
     * Envía el formulario "Contactar soporte" como correo al equipo de soporte.
     *
     * @param asunto    Asunto de la consulta.
     * @param categoria Categoría seleccionada (Cuenta, Pagos, etc.).
     * @param mensaje   Cuerpo del mensaje del usuario.
     * @param adjunto   Imagen adjunta opcional (JPG, PNG, WEBP · máx. 5 MB). Puede ser {@code null}.
     */
    void enviarEmailSoporte(String asunto, String categoria, String mensaje, MultipartFile adjunto);

    /**
     * Envía el formulario "Reportar un problema" como correo al equipo técnico.
     *
     * @param descripcionCorta Resumen breve del problema.
     * @param prioridad        Nivel de prioridad: baja, media o alta.
     * @param seccion          Sección de la app donde ocurrió el problema.
     * @param descripcion      Descripción detallada.
     * @param queHacias        Contexto del usuario (puede ser {@code null} o vacío).
     * @param adjunto          Imagen adjunta opcional (JPG, PNG, WEBP · máx. 5 MB). Puede ser {@code null}.
     */
    void enviarEmailReporte(String descripcionCorta, String prioridad, String seccion,
                            String descripcion, String queHacias, MultipartFile adjunto);
}

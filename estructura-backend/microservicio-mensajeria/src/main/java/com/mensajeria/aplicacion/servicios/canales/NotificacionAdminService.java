package com.mensajeria.aplicacion.servicios.canales;

/**
 * Servicio dedicado para notificaciones críticas y administrativas.
 * <p>
 * Extraído de NotificacionService para cumplir con el Principio de Segregación
 * de Interfaces (ISP) y Liskov Substitution Principle (LSP).
 * </p>
 */
public interface NotificacionAdminService {

    /**
     * Envía un correo electrónico al administrador del sistema para alertas
     * críticas.
     * No utiliza plantillas de usuario para garantizar la entrega de logs técnicos.
     *
     * @param destinatario Email del administrador.
     * @param asunto       Asunto del correo.
     * @param cuerpo       Contenido del mensaje (Texto plano o HTML).
     * @param esHtml       Indica si el cuerpo debe procesarse como HTML.
     */
    void enviarEmailAdministrador(String destinatario, String asunto, String cuerpo, boolean esHtml);
}

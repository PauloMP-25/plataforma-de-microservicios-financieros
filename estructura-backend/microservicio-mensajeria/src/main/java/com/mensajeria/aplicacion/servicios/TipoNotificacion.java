package com.mensajeria.aplicacion.servicios;

/**
 * Enumeración de los canales de notificación disponibles en el sistema.
 * <p>
 * Permite que {@link NotificacionService} sea completamente agnóstico al
 * medio de entrega: la implementación concreta decide cómo enviar el mensaje
 * (SMTP, Twilio, etc.) según el tipo recibido.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public enum TipoNotificacion {

    /**
     * Notificación enviada por correo electrónico mediante SMTP (Thymeleaf).
     * La clave {@code "destinatario"} del mapa de variables debe contener el email.
     */
    EMAIL,

    /**
     * Notificación enviada por SMS mediante Twilio.
     * La clave {@code "destinatario"} del mapa de variables debe contener el
     * número de teléfono en formato E.164.
     */
    SMS,

    /**
     * Notificación enviada por WhatsApp mediante Meta Cloud API.
     */
    WHATSAPP
}

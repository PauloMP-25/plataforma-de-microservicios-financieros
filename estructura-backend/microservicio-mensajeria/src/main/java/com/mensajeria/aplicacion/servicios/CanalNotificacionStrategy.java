package com.mensajeria.aplicacion.servicios;

import java.util.Map;

/**
 * Interfaz base para el patrón Strategy de canales de notificación.
 * <p>
 * Cada implementación (Email, SMS, WhatsApp) define cómo enviar el mensaje
 * según sus propios protocolos y proveedores.
 * </p>
 */
public interface CanalNotificacionStrategy {

    /**
     * Envía una notificación a través del canal específico.
     * 
     * @param destinatario Dirección de correo, número de teléfono, etc.
     * @param variables    Mapa de datos dinámicos (código, nombre, etc).
     */
    void enviar(String destinatario, Map<String, Object> variables);

    /**
     * Determina si esta estrategia es aplicable para un tipo de notificación.
     */
    boolean soporta(TipoNotificacion tipo);
}

package com.mensajeria.aplicacion.servicios;

import java.util.Map;

/**
 * Contrato para el envío de mensajes a través de WhatsApp Business Cloud API.
 * <p>
 * Define las operaciones para enviar notificaciones basadas en plantillas (Templates),
 * cumpliendo con las políticas de Meta para el inicio de conversaciones.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.0.0
 * @since 2026-05
 */
public interface IWhatsAppService {

    /**
     * Envía un mensaje de WhatsApp basado en una plantilla pre-aprobada en Meta.
     *
     * @param telefono   Número de destino en formato E.164 (ej. +51987654321).
     * @param plantilla  Nombre de la plantilla configurada en el Business Manager.
     * @param variables  Mapa de parámetros para completar los placeholders {{1}}, {{2}}, etc.
     * @throws com.mensajeria.aplicacion.excepciones.MensajeriaExternaException
     *             si la API de Meta retorna un error o el token ha expirado.
     */
    void enviarMensajeTemplate(String telefono, String plantilla, Map<String, String> variables);

    /**
     * Verifica si el número tiene el formato adecuado para WhatsApp Cloud API.
     * 
     * @param telefono Número de teléfono a validar.
     * @return true si es un formato E.164 válido.
     */
    boolean esNumeroValido(String telefono);
}

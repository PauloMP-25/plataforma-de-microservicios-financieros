package com.mensajeria.aplicacion.servicios;

import java.util.Map;

/**
 * Contrato unificado del servicio de notificaciones del microservicio de mensajería.
 * <p>
 * Esta interfaz es <strong>agnóstica al canal</strong>: no sabe si el mensaje
 * se enviará por email o SMS. Cada implementación concreta en
 * {@code com.mensajeria.aplicacion.servicios.impl} decide cómo despachar la
 * notificación según el {@link TipoNotificacion} recibido.
 * </p>
 * <p>
 * El mapa {@code variables} es abierto por diseño: permite que cada
 * implementación extraiga lo que necesita ({@code "codigo"}, {@code "appName"},
 * {@code "proposito"}, etc.) sin acoplar la interfaz a DTOs concretos.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface NotificacionService {

    /**
     * Envía una notificación al destinatario usando el canal y las variables
     * proporcionadas.
     *
     * @param tipo         Canal de envío ({@link TipoNotificacion#EMAIL} o
     *                     {@link TipoNotificacion#SMS}). La implementación
     *                     deberá lanzar {@link UnsupportedOperationException}
     *                     si no soporta el tipo recibido.
     * @param destinatario Dirección de destino: email en formato RFC 5321 para
     *                     {@code EMAIL}, o número en formato E.164 para {@code SMS}.
     * @param variables    Mapa de parámetros de la plantilla. Claves estándar:<br>
     *                     &nbsp;• {@code "codigo"} — OTP de 6 dígitos (requerido).<br>
     *                     &nbsp;• {@code "proposito"} — {@code PropositoCodigo} del OTP.<br>
     *                     &nbsp;• {@code "appName"} — nombre de la app para el asunto del correo.
     * @throws com.mensajeria.aplicacion.excepciones.MensajeriaExternaException
     *             si el proveedor externo (SMTP, Twilio) rechaza el envío.
     */
    void enviar(TipoNotificacion tipo, String destinatario, Map<String, Object> variables);
}

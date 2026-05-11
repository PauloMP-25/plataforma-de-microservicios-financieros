package com.mensajeria.aplicacion.servicios;

/**
 * Contrato del servicio de throttling de mensajería.
 * <p>
 * Define la operación de validación de límite de intentos por canal, permitiendo
 * que la implementación concreta en Redis sea intercambiable en testing.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface IThrottlingService {

    /**
     * Valida y registra un intento de envío de código para el canal e
     * identificador dados. Si el contador acumulado en Redis supera 3, lanza
     * {@code LimiteIntentosExcedidoException}.
     *
     * @param canal         Canal de notificación usado ({@code "email"} o
     *                      {@code "sms"}), que forma parte de la clave Redis.
     * @param identificador Identificador único del usuario en ese canal (email,
     *                      número de teléfono o UUID).
     * @throws com.mensajeria.aplicacion.excepciones.LimiteIntentosExcedidoException
     *             cuando el número de intentos acumulados supera el límite
     *             permitido para ese canal.
     */
    void registrarIntento(String canal, String identificador);
}

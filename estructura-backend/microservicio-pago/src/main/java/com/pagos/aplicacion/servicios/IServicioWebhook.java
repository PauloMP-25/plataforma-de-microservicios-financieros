package com.pagos.aplicacion.servicios;

import com.stripe.model.Event;

/**
 * Interfaz para el procesamiento de eventos asíncronos (Webhooks) de Stripe.
 */
public interface IServicioWebhook {

    /**
     * Procesa un evento verificado de Stripe.
     * Implementa lógica de idempotencia para evitar duplicados.
     * 
     * @param evento Objeto Event del SDK de Stripe.
     */
    void procesarEvento(Event evento);
}

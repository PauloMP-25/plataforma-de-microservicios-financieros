package com.suscripciones.dominio.excepciones;

/**
 * Excepción lanzada cuando ocurre un error lógico o de validación irrecuperable
 * durante el procesamiento de un evento de la bandeja de salida (Outbox).
 * Evita reintentos inútiles del scheduler.
 */
public class ExcepcionLogicaOutbox extends RuntimeException {
    
    public ExcepcionLogicaOutbox(String mensaje) {
        super(mensaje);
    }
}

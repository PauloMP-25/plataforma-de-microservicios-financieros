package com.mensajeria.aplicacion.excepciones;

public class MensajeriaExternaException extends RuntimeException {
    private final String detalle;

    public MensajeriaExternaException(String mensaje, String detalle) {
        super(mensaje);
        this.detalle = detalle;
    }
}

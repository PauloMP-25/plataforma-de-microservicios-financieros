package com.mensajeria.aplicacion.excepciones;

public class MensajeriaExternaException extends RuntimeException {
    @SuppressWarnings("unused")
    private final String detalle;

    public MensajeriaExternaException(String mensaje, String detalle) {
        super(mensaje);
        this.detalle = detalle;
    }
}
